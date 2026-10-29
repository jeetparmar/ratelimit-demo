package com.example.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;
    private final long capacity;
    private final double refillPerSec;

    public RateLimitFilter(RateLimiter limiter,
                           @Value("${ratelimit.capacity:20}") long capacity,
                           @Value("${ratelimit.refill-per-sec:5}") double refillPerSec) {
        this.limiter = limiter;
        this.capacity = capacity;
        this.refillPerSec = refillPerSec;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        var result = limiter.tryAcquire(clientKey(req), capacity, refillPerSec);

        res.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        if (result.remaining() >= 0) {
            res.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        }

        if (!result.allowed()) {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(Math.max(1, (result.retryAfterMs() + 999) / 1000)));
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"rate_limit_exceeded\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    private String clientKey(HttpServletRequest req) {
        String apiKey = req.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) return "key:" + apiKey;
        // Behind a proxy, set server.forward-headers-strategy=native so this is the real client IP
        return "ip:" + req.getRemoteAddr();
    }
}
