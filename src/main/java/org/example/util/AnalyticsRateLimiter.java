package org.example.util;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.util.concurrent.*;

@Component
public class AnalyticsRateLimiter implements Filter {
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Value("${analytics.rate-limiter.tokens-per-second:200}")
    private double tokensPerSecond;

    @Value("${analytics.rate-limiter.bucket-size:400}")
    private int bucketSize;

    @PostConstruct
    public void init() {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        // Only apply limiter to ingestion endpoint
        String path = r.getRequestURI();
        if (!"/api/events".equals(path)) {
            chain.doFilter(req, resp);
            return;
        }

        String client = r.getHeader("X-Client-Id");
        if (client == null || client.isBlank()) client = "anonymous";
        TokenBucket b = buckets.computeIfAbsent(client, k -> new TokenBucket(tokensPerSecond, bucketSize));
        if (!b.tryConsume()) {
            HttpServletResponse response = (HttpServletResponse) resp;
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate_limit_exceeded\"}");
            return;
        }
        chain.doFilter(req, resp);
    }

    static class TokenBucket {
        private final double refillPerMillis;
        private final int capacity;
        private double tokens;
        private long lastRefill;

        TokenBucket(double tokensPerSecond, int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefill = System.currentTimeMillis();
            this.refillPerMillis = tokensPerSecond / 1000.0;
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long delta = now - lastRefill;
            if (delta <= 0) return;
            tokens = Math.min(capacity, tokens + delta * refillPerMillis);
            lastRefill = now;
        }
    }
}


