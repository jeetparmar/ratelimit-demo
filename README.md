# Redis rate limiting demo (Spring Boot 3 + Java 21)

Token-bucket limiter backed by Redis (atomic Lua script), applied to all requests via a servlet filter.

## Run
```bash
docker compose up --build
./test.sh
```

Expected: ~10 × `200` (burst capacity), then `429` until tokens refill (2/sec).

## Config (env vars in docker-compose.yml)
- `RATELIMIT_CAPACITY` – max burst size (default 10)
- `RATELIMIT_REFILL` – tokens added per second (default 2)

Clients are keyed by the `X-API-Key` header, else by IP:
```bash
curl -H "X-API-Key: alice" localhost:8080/api/hello -i   # see X-RateLimit-* headers
```

## Local dev without Docker for the app
```bash
docker compose up redis -d
./mvnw spring-boot:run     # or: mvn spring-boot:run
```

## Layout
- `RateLimitFilter` – per-request check, sets 429 + `Retry-After`
- `RateLimiter` – runs `scripts/token_bucket.lua` in Redis; fails open if Redis is down
- `DemoController` – `GET /api/hello`
