package org.example.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

class AnalyticsRateLimiterTest {
    private AnalyticsRateLimiter limiter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private PrintWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        limiter = new AnalyticsRateLimiter();
        // Use reflection to set private fields
        java.lang.reflect.Field tokensField = AnalyticsRateLimiter.class.getDeclaredField("tokensPerSecond");
        tokensField.setAccessible(true);
        tokensField.set(limiter, 2);
        java.lang.reflect.Field bucketField = AnalyticsRateLimiter.class.getDeclaredField("bucketSize");
        bucketField.setAccessible(true);
        bucketField.set(limiter, 4);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void doFilter_shouldPassNonEventPath() throws Exception {
        when(request.getRequestURI()).thenReturn("/other");
        limiter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_shouldRateLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getHeader("X-Client-Id")).thenReturn("test-client");
        // Exhaust tokens
        for (int i = 0; i < 5; i++) {
            limiter.doFilter(request, response, chain);
        }
        verify(response, atLeastOnce()).setStatus(429);
        verify(writer, atLeastOnce()).write(contains("rate_limit_exceeded"));
    }
}
