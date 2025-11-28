package org.example.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.SetOperations;
import java.time.Instant;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AnalyticsDataRepositoryTest {
    private StringRedisTemplate redis;
    private AnalyticsDataRepository repo;
    private ZSetOperations<String, String> zsetOps;
    private SetOperations<String, String> setOps;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        zsetOps = mock(ZSetOperations.class);
        setOps = mock(SetOperations.class);
        when(redis.opsForZSet()).thenReturn(zsetOps);
        when(redis.opsForSet()).thenReturn(setOps);
        repo = new AnalyticsDataRepository(redis);
    }

    @Test
    void markActiveUser_shouldAddAndRemove() {
        UUID userId = UUID.randomUUID();
        Instant ts = Instant.now();
        repo.markActiveUser(userId, ts);
        verify(zsetOps).add(anyString(), eq(userId.toString()), anyDouble());
    }

    @Test
    void addPageView_shouldAddAndExpire() {
        repo.addPageView("/home", Instant.now());
        verify(zsetOps).add(anyString(), anyString(), anyDouble());
        verify(redis).expire(anyString(), any());
    }

    @Test
    void addSessionForUser_shouldAddAndExpire() {
        repo.addSessionForUser("user1", "session1");
        verify(setOps).add(anyString(), eq("session1"));
        verify(redis).expire(anyString(), any());
    }

    @Test
    void markActiveUser_shouldThrowOnNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> repo.markActiveUser(null, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> repo.markActiveUser(UUID.randomUUID(), null));
    }

    @Test
    void countActiveUsers_shouldReturnZeroOnNegativeNowMs() {
        assertEquals(0, repo.countActiveUsers(-1));
    }

    @Test
    void addPageView_shouldThrowOnNullOrEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> repo.addPageView(null, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> repo.addPageView("", Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> repo.addPageView("/home", null));
    }

    @Test
    void countPageViews_shouldReturnZeroOnInvalidInputs() {
        assertEquals(0, repo.countPageViews(null, System.currentTimeMillis()));
        assertEquals(0, repo.countPageViews("", System.currentTimeMillis()));
        assertEquals(0, repo.countPageViews("/home", -1));
    }

    @Test
    void addSessionForUser_shouldThrowOnNullOrEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> repo.addSessionForUser(null, "session1"));
        assertThrows(IllegalArgumentException.class, () -> repo.addSessionForUser("", "session1"));
        assertThrows(IllegalArgumentException.class, () -> repo.addSessionForUser("user1", null));
        assertThrows(IllegalArgumentException.class, () -> repo.addSessionForUser("user1", ""));
    }

    @Test
    void countSessionsForUser_shouldReturnZeroOnNullOrEmptyUserId() {
        assertEquals(0, repo.countSessionsForUser(null));
        assertEquals(0, repo.countSessionsForUser(""));
    }
}
