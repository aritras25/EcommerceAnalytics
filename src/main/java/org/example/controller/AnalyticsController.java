package org.example.controller;


import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.example.dto.UserEventInputRequest;
import org.example.repository.AnalyticsDataRepository;
import org.example.service.AnalyticsService;
import org.example.util.MockEventGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/apis/v1")
@Timed(value = "analytics.controller", percentiles ={0.95, 0.99}, histogram = true)
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final AnalyticsDataRepository repository;
    private final MockEventGenerator mockEventGenerator;

    public AnalyticsController(AnalyticsService analyticsService, AnalyticsDataRepository repository, MockEventGenerator mockEventGenerator) {
        this.analyticsService = analyticsService;
        this.repository = repository;
        this.mockEventGenerator = mockEventGenerator;
    }

    @PostMapping("/events")
    public ResponseEntity<?> ingest(@Valid @RequestBody UserEventInputRequest ev) {
        long now = Instant.now().toEpochMilli();
        if (ev.getEventTimestamp() != null && ev.getEventTimestamp().toEpochMilli() - now > 120_000) {
            return ResponseEntity.badRequest().body(Map.of("error", "timestamp_too_future"));
        }
        analyticsService.process(ev);
        return ResponseEntity.accepted().body(Map.of("status", "accepted"));
    }

    @GetMapping("/metrics/active-users")
    public ResponseEntity<?> activeUsers() {
        long now = Instant.now().toEpochMilli();
        long count = repository.countActiveUsers(now);
        return ResponseEntity.ok(Map.of("activeUsersLast5m", count));
    }

    @GetMapping("/metrics/pageviews")
    public ResponseEntity<?> pageviews(@RequestParam("url") String url) {
        long now = Instant.now().toEpochMilli();
        long c = repository.countPageViews(url, now);
        return ResponseEntity.ok(Map.of("url", url, "pageViewsLast15m", c));
    }

    @GetMapping("/metrics/active-sessions")
    public ResponseEntity<?> activeSessions(@RequestParam("userId") String userId) {
        long c = repository.countSessionsForUser(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "activeSessionsLast5m", c));
    }
}

