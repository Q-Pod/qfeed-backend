const express = require("express");
const Redis = require("ioredis");

const app = express();

app.use(express.urlencoded({ extended: false }));
app.use(express.json());

const port = Number(process.env.PORT || 18089);
const redisUrl = process.env.REDIS_URL || "redis://localhost:6379";
const redisPrefix = process.env.REDIS_PREFIX || "loadtest:kakao:";
const tokenTtlSeconds = Number(process.env.TOKEN_TTL_SECONDS || 3600);
const idBase = Number(process.env.ID_BASE || 9000000);

const redis = new Redis(redisUrl, {
  lazyConnect: true,
  maxRetriesPerRequest: 3
});

const sequenceKey = `${redisPrefix}seq`;
const tokenKeyPrefix = `${redisPrefix}token:`;

function parseSeqFromCode(code) {
  if (!code) return null;
  const match = code.match(/^lt-code-(\d+)$/);
  if (!match) return null;
  return Number(match[1]);
}

function buildSeqLabel(seq) {
  return String(seq).padStart(6, "0");
}

function getBearerToken(authHeader) {
  if (!authHeader) return null;
  const parts = authHeader.split(" ");
  if (parts.length !== 2) return null;
  if (!/^Bearer$/i.test(parts[0])) return null;
  return parts[1];
}

app.get("/health", async (_req, res) => {
  try {
    await redis.ping();
    res.status(200).json({ status: "UP" });
  } catch (error) {
    res.status(503).json({ status: "DOWN", error: error.message });
  }
});

app.get("/oauth/authorize", async (req, res) => {
  const redirectUri = req.query.redirect_uri;
  const state = req.query.state;

  if (!redirectUri) {
    return res.status(400).json({ error: "redirect_uri is required" });
  }

  try {
    const seq = await redis.incr(sequenceKey);
    const code = `lt-code-${seq}`;

    const url = new URL(redirectUri);
    url.searchParams.set("code", code);
    if (state) {
      url.searchParams.set("state", state);
    }

    return res.redirect(302, url.toString());
  } catch (error) {
    return res.status(500).json({ error: "failed to issue auth code", reason: error.message });
  }
});

app.post("/oauth/token", async (req, res) => {
  const grantType = req.body.grant_type;
  const code = req.body.code;

  if (grantType !== "authorization_code") {
    return res.status(400).json({ error: "unsupported_grant_type" });
  }
  if (!code) {
    return res.status(400).json({ error: "invalid_request", reason: "code is required" });
  }

  try {
    let seq = parseSeqFromCode(code);
    if (seq === null) {
      seq = await redis.incr(sequenceKey);
    }

    const accessToken = `wm-access-token-${seq}`;
    const refreshToken = `wm-refresh-token-${seq}`;
    await redis.setex(`${tokenKeyPrefix}${accessToken}`, tokenTtlSeconds, String(seq));

    return res.status(200).json({
      access_token: accessToken,
      token_type: "bearer",
      refresh_token: refreshToken,
      expires_in: tokenTtlSeconds,
      scope: "profile_nickname profile_image"
    });
  } catch (error) {
    return res.status(500).json({ error: "token_issue_failed", reason: error.message });
  }
});

app.get("/v2/user/me", async (req, res) => {
  const accessToken = getBearerToken(req.header("Authorization"));
  if (!accessToken) {
    return res.status(401).json({ msg: "missing access token" });
  }

  try {
    const seqRaw = await redis.get(`${tokenKeyPrefix}${accessToken}`);
    if (!seqRaw) {
      return res.status(401).json({ msg: "invalid access token" });
    }

    const seq = Number(seqRaw);
    const label = buildSeqLabel(seq);
    const userId = idBase + seq;

    return res.status(200).json({
      id: userId,
      kakao_account: {
        email: `loadtest+${label}@qfeed.local`,
        profile: {
          nickname: `loadtest-user-${label}`,
          profile_image_url: `https://cdn.qfeed.local/profile/${label}.png`
        }
      }
    });
  } catch (error) {
    return res.status(500).json({ msg: "failed to resolve user", reason: error.message });
  }
});

async function start() {
  try {
    await redis.connect();
    await redis.ping();
    app.listen(port, () => {
      // eslint-disable-next-line no-console
      console.log(`[oauth-mock] listening on :${port}, redis=${redisUrl}`);
    });
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error("[oauth-mock] startup failed:", error);
    process.exit(1);
  }
}

start();
