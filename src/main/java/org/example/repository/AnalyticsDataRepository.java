package org.example.repository;


import org.springframework.stereotype.Repository;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Repository
public class AnalyticsDataRepository {
    private final StringRedisTemplate redis;
    private static final long FIVE_MIN_MS = Duration.ofMinutes(5).toMillis();
    private static final long FIFTEEN_MIN_MS = Duration.ofMinutes(15).toMillis();

    public AnalyticsDataRepository(StringRedisTemplate redis) { this.redis = redis; }

    private final String ACTIVE_USERS_KEY = "active_users";

    public void markActiveUser(UUID userId, Instant timestampMs) {
        if (userId == null || timestampMs == null) {
            throw new IllegalArgumentException("userId and timestampMs must not be null");
        }
        redis.opsForZSet().add(ACTIVE_USERS_KEY, userId.toString(), timestampMs.toEpochMilli());
        long max = timestampMs.getEpochSecond() - FIVE_MIN_MS - 1000;
        if (max > 0) redis.opsForZSet().removeRangeByScore(ACTIVE_USERS_KEY, 0, max);
    }

    public long countActiveUsers(long nowMs) {
        if (nowMs < 0) return 0;
        long minScore = nowMs - FIVE_MIN_MS;
        Long c = redis.opsForZSet().count(ACTIVE_USERS_KEY, minScore, Double.POSITIVE_INFINITY);
        return c == null ? 0 : c;
    }

    private String pageKey(String url) {
        if (url == null || url.isEmpty()) {
            return "pv:invalid";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] h = md.digest(url.getBytes(StandardCharsets.UTF_8));
            return "pv:" + HexFormat.of().formatHex(h).substring(0, 20);
        } catch (Exception e) {
            return "pv:" + Math.abs(url.hashCode());
        }
    }

    public void addPageView(String url, Instant timestampMs) {
        if (url == null || url.isEmpty() || timestampMs == null) {
            throw new IllegalArgumentException("url and timestampMs must not be null or empty");
        }
        String key = pageKey(url);
        redis.opsForZSet().add(key, UUID.randomUUID().toString(), timestampMs.getEpochSecond());
        redis.expire(key, Duration.ofMinutes(20));
    }

    public long countPageViews(String url, long nowMs) {
        if (url == null || url.isEmpty() || nowMs < 0) return 0;
        String key = pageKey(url);
        long min = nowMs - FIFTEEN_MIN_MS;
        Long c = redis.opsForZSet().count(key, min, Double.POSITIVE_INFINITY);
        return c == null ? 0 : c;
    }

    public void addSessionForUser(String userId, String sessionId) {
        if (userId == null || userId.isEmpty() || sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("userId and sessionId must not be null or empty");
        }
        String key = "sessions:" + userId;
        redis.opsForSet().add(key, sessionId);
        redis.expire(key, Duration.ofMinutes(5));
    }

    public long countSessionsForUser(String userId) {
        if (userId == null || userId.isEmpty()) return 0;
        String key = "sessions:" + userId;
        Long c = redis.opsForSet().size(key);
        return c == null ? 0 : c;
    }
}
