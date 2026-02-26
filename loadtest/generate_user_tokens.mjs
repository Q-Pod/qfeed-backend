#!/usr/bin/env node

import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

function printHelp() {
  console.log(`Usage:
  node k6/generate_user_tokens.mjs [options]

Options:
  --user-count <number>        number of users to generate (default: 100)
  --start-user-id <number>     first user id (default: 1)
  --out-dir <path>             output directory (default: k6/generated)
  --access-exp-sec <number>    access token expiry seconds (default: 21600)
  --refresh-exp-sec <number>   refresh token expiry seconds (default: 1209600)
  -h, --help                   show this help

Environment fallbacks:
  USER_COUNT, START_USER_ID, OUT_DIR, ACCESS_EXP_SEC, REFRESH_EXP_SEC
`);
}

function parseArgs(argv) {
  const options = {
    userCount: 100,
    startUserId: 1,
    outDir: "k6/generated",
    accessExpSec: 21600,
    refreshExpSec: 1209600,
  };
  let userCountProvided = false;

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    const next = argv[i + 1];

    if (arg === "-h" || arg === "--help") {
      printHelp();
      process.exit(0);
    }
    if (arg.startsWith("--user-count=")) {
      options.userCount = Number(arg.slice("--user-count=".length));
      userCountProvided = true;
      continue;
    }
    if (arg === "--user-count" && next) {
      options.userCount = Number(next);
      userCountProvided = true;
      i += 1;
      continue;
    }
    if (arg === "-n" && next) {
      options.userCount = Number(next);
      userCountProvided = true;
      i += 1;
      continue;
    }
    if (arg.startsWith("--start-user-id=")) {
      options.startUserId = Number(arg.slice("--start-user-id=".length));
      continue;
    }
    if (arg === "--start-user-id" && next) {
      options.startUserId = Number(next);
      i += 1;
      continue;
    }
    if (arg.startsWith("--out-dir=")) {
      options.outDir = arg.slice("--out-dir=".length);
      continue;
    }
    if (arg === "--out-dir" && next) {
      options.outDir = next;
      i += 1;
      continue;
    }
    if (arg.startsWith("--access-exp-sec=")) {
      options.accessExpSec = Number(arg.slice("--access-exp-sec=".length));
      continue;
    }
    if (arg === "--access-exp-sec" && next) {
      options.accessExpSec = Number(next);
      i += 1;
      continue;
    }
    if (arg.startsWith("--refresh-exp-sec=")) {
      options.refreshExpSec = Number(arg.slice("--refresh-exp-sec=".length));
      continue;
    }
    if (arg === "--refresh-exp-sec" && next) {
      options.refreshExpSec = Number(next);
      i += 1;
      continue;
    }
  }

  if (!userCountProvided && process.env.USER_COUNT) {
    options.userCount = Number(process.env.USER_COUNT);
  }
  if (process.env.START_USER_ID) {
    options.startUserId = Number(process.env.START_USER_ID);
  }
  if (process.env.OUT_DIR) {
    options.outDir = process.env.OUT_DIR;
  }
  if (process.env.ACCESS_EXP_SEC) {
    options.accessExpSec = Number(process.env.ACCESS_EXP_SEC);
  }
  if (process.env.REFRESH_EXP_SEC) {
    options.refreshExpSec = Number(process.env.REFRESH_EXP_SEC);
  }

  return options;
}

function parseDotEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {};
  }

  const result = {};
  const lines = fs.readFileSync(filePath, "utf8").split(/\r?\n/);
  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) {
      continue;
    }
    const idx = line.indexOf("=");
    if (idx < 0) {
      continue;
    }
    const key = line.slice(0, idx).trim();
    let value = line.slice(idx + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    result[key] = value;
  }
  return result;
}

function toBase64Url(input) {
  return Buffer.from(input)
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function signHs256(unsignedToken, secret) {
  return crypto
    .createHmac("sha256", secret)
    .update(unsignedToken)
    .digest("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function createJwt({ userId, nickname, issuer, tokenType, familyUuid, expSec, nowSec, secret }) {
  const header = {
    alg: "HS256",
    typ: "JWT",
  };

  const payload = {
    jti: crypto.randomUUID(),
    sub: String(userId),
    userId,
    nickname,
    type: tokenType,
    iss: issuer,
    iat: nowSec,
    exp: nowSec + expSec,
  };

  if (tokenType === "ACCESS") {
    payload.roles = ["ROLE_USER"];
  }

  if (tokenType === "REFRESH") {
    payload.familyUuid = familyUuid;
  }

  const encodedHeader = toBase64Url(JSON.stringify(header));
  const encodedPayload = toBase64Url(JSON.stringify(payload));
  const unsignedToken = `${encodedHeader}.${encodedPayload}`;
  const signature = signHs256(unsignedToken, secret);
  return `${unsignedToken}.${signature}`;
}

function tokenHash(refreshToken) {
  return crypto.createHash("sha256").update(refreshToken).digest("base64");
}

function toSqlTimestamp(utcDate) {
  return utcDate.toISOString().slice(0, 19).replace("T", " ");
}

function csvEscape(value) {
  const str = String(value ?? "");
  if (str.includes('"') || str.includes(",") || str.includes("\n")) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

function writeCsv(filePath, headers, rows) {
  const lines = [];
  lines.push(headers.join(","));
  for (const row of rows) {
    lines.push(headers.map((h) => csvEscape(row[h])).join(","));
  }
  fs.writeFileSync(filePath, `${lines.join("\n")}\n`, "utf8");
}

function main() {
  const options = parseArgs(process.argv.slice(2));
  if (!Number.isInteger(options.userCount) || options.userCount <= 0) {
    throw new Error("--user-count must be a positive integer");
  }
  if (!Number.isInteger(options.startUserId) || options.startUserId <= 0) {
    throw new Error("--start-user-id must be a positive integer");
  }
  if (!Number.isInteger(options.accessExpSec) || options.accessExpSec <= 0) {
    throw new Error("--access-exp-sec must be a positive integer");
  }
  if (!Number.isInteger(options.refreshExpSec) || options.refreshExpSec <= 0) {
    throw new Error("--refresh-exp-sec must be a positive integer");
  }

  const projectRoot = process.cwd();
  const envFromFile = parseDotEnvFile(path.join(projectRoot, ".env"));
  const secret = process.env.JWT_SECRET || envFromFile.JWT_SECRET;
  const issuer = process.env.JWT_ISSUER || envFromFile.JWT_ISSUER || "QFeed";

  if (!secret) {
    throw new Error("JWT secret not found. Set JWT_SECRET env or .env JWT_SECRET");
  }

  const outDirAbs = path.resolve(projectRoot, options.outDir);
  fs.mkdirSync(outDirAbs, { recursive: true });

  const nowSec = Math.floor(Date.now() / 1000);
  const nowDate = new Date(nowSec * 1000);

  const tokenRows = [];
  const accessRows = [];

  for (let i = 0; i < options.userCount; i += 1) {
    const userId = options.startUserId + i;
    const nickname = `lt_user_${String(userId).padStart(3, "0")}`;
    const familyUuid = crypto.randomUUID();

    const accessToken = createJwt({
      userId,
      nickname,
      issuer,
      tokenType: "ACCESS",
      familyUuid: null,
      expSec: options.accessExpSec,
      nowSec,
      secret,
    });

    const refreshToken = createJwt({
      userId,
      nickname,
      issuer,
      tokenType: "REFRESH",
      familyUuid,
      expSec: options.refreshExpSec,
      nowSec,
      secret,
    });

    const accessExpiresAt = new Date((nowSec + options.accessExpSec) * 1000);
    const refreshExpiresAt = new Date((nowSec + options.refreshExpSec) * 1000);

    tokenRows.push({
      user_id: userId,
      nickname,
      family_uuid: familyUuid,
      access_token: accessToken,
      refresh_token: refreshToken,
      refresh_token_hash: tokenHash(refreshToken),
      issued_at: toSqlTimestamp(nowDate),
      access_expires_at: toSqlTimestamp(accessExpiresAt),
      refresh_expires_at: toSqlTimestamp(refreshExpiresAt),
    });

    accessRows.push({
      user_id: userId,
      access_token: accessToken,
      refresh_token: refreshToken,
      access_expires_at: toSqlTimestamp(accessExpiresAt),
    });
  }

  writeCsv(
    path.join(outDirAbs, "user-token-list.csv"),
    [
      "user_id",
      "nickname",
      "family_uuid",
      "access_token",
      "refresh_token",
      "refresh_token_hash",
      "issued_at",
      "access_expires_at",
      "refresh_expires_at",
    ],
    tokenRows
  );

  writeCsv(
    path.join(outDirAbs, "user-access-token-list.csv"),
    ["user_id", "access_token", "refresh_token", "access_expires_at"],
    accessRows
  );

  console.log(`generated: ${path.join(outDirAbs, "user-token-list.csv")}`);
  console.log(`generated: ${path.join(outDirAbs, "user-access-token-list.csv")}`);
  console.log(
    `summary: users=${options.userCount}, startUserId=${options.startUserId}, issuer=${issuer}, accessExpSec=${options.accessExpSec}, refreshExpSec=${options.refreshExpSec}`
  );
}

main();
