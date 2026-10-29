package com.example.ratelimit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RateLimiter {

    public record Result(boolean allowed, long remaining, long retryAfterMs) {}

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        this.script.setResultType(List.class);
    }

    /** Token bucket: `capacity` burst, refilled at `refillPerSec`. */
    public Result tryAcquire(String key, long capacity, double refillPerSec) {
        try {
            List<?> r = redis.execute(script, List.of("rl:" + key),
                    String.valueOf(capacity), String.valueOf(refillPerSec), "1");
            return new Result(((Long) r.get(0)) == 1L, (Long) r.get(1), (Long) r.get(2));
        } catch (Exception e) {
            // Fail-open: don't take the API down if Redis is unavailable
            return new Result(true, -1, 0);
        }
    }
}
