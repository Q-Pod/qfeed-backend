# Load Test OAuth Mock (Redis-backed)

This setup runs a standalone Kakao OAuth mock server backed by Redis to issue
monotonic user identities for load testing.

## Start

```bash
cd docker/loadtest
docker compose -f oauth-mock-compose.yaml up -d --build
```

## Stop

```bash
cd docker/loadtest
docker compose -f oauth-mock-compose.yaml down -v
```

## Smoke Test

```bash
curl -i "http://localhost:18089/health"

curl -s -X POST "http://localhost:18089/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=test-code"

curl -s "http://localhost:18089/v2/user/me" \
  -H "Authorization: Bearer wm-access-token-1"
```

## Behavior

- `POST /oauth/token`
  - if code is `lt-code-{N}`: uses `N`
  - otherwise: allocates sequence via Redis `INCR`
- `GET /v2/user/me`
  - resolves sequence from token mapping
  - returns:
    - `id = ID_BASE + seq` (default base: 9000000)
    - `email = loadtest+{seqPad}@qfeed.local`
    - `nickname = loadtest-user-{seqPad}`

## Env

- `PORT` (default `18089`)
- `REDIS_URL` (default `redis://localhost:6379`)
- `REDIS_PREFIX` (default `loadtest:kakao:`)
- `TOKEN_TTL_SECONDS` (default `3600`)
- `ID_BASE` (default `9000000`)
