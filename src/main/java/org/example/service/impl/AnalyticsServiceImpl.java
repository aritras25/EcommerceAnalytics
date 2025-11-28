package org.example.service.impl;

import org.example.dto.UserEventInputRequest;
import org.example.repository.AnalyticsDataRepository;
import org.example.service.AnalyticsService;
import org.example.util.SessionIdGenerator;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {
    private final AnalyticsDataRepository repository;

    public AnalyticsServiceImpl(AnalyticsDataRepository repository) { this.repository = repository; }

    public void process(UserEventInputRequest ev) {
        Instant ts = ev.getEventTimestamp() == null ? Instant.now() : ev.getEventTimestamp();
        repository.markActiveUser(ev.getUserId(), ts);
        repository.addPageView(ev.getPageUrl(), ts);
        String sid = ev.getSessionId();
        if (sid == null || sid.isBlank()) sid = SessionIdGenerator.generate(ev.getUserId().toString(), ev.getPageUrl(), ts);
        repository.addSessionForUser(ev.getUserId().toString(), sid);
    }
}

