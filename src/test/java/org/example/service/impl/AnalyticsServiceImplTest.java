package org.example.service.impl;

import org.example.dto.UserEventInputRequest;
import org.example.repository.AnalyticsDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

class AnalyticsServiceImplTest {
    private AnalyticsDataRepository repository;
    private AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(AnalyticsDataRepository.class);
        service = new AnalyticsServiceImpl(repository);
    }

    @Test
    void process_shouldHandleNullSessionId() {
        UserEventInputRequest req = mock(UserEventInputRequest.class);
        when(req.getUserId()).thenReturn(UUID.randomUUID());
        when(req.getPageUrl()).thenReturn("/home");
        when(req.getEventTimestamp()).thenReturn(null);
        when(req.getSessionId()).thenReturn(null);
        service.process(req);
        verify(repository).markActiveUser(any(), any());
        verify(repository).addPageView(any(), any());
        verify(repository).addSessionForUser(anyString(), anyString());
    }

    @Test
    void process_shouldUseProvidedSessionId() {
        UserEventInputRequest req = mock(UserEventInputRequest.class);
        when(req.getUserId()).thenReturn(UUID.randomUUID());
        when(req.getPageUrl()).thenReturn("/home");
        when(req.getEventTimestamp()).thenReturn(Instant.now());
        when(req.getSessionId()).thenReturn("session123");
        service.process(req);
        verify(repository).addSessionForUser(anyString(), eq("session123"));
    }
}
