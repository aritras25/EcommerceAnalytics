package org.example.service;

import org.example.dto.UserEventInputRequest;

public interface AnalyticsService {
    void process(UserEventInputRequest ev);
}
