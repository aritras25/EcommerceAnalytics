package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.EventType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEventInputRequest {
    private UUID userId;
    private EventType eventType;
    private Instant eventTimestamp;
    private String pageUrl;
    private String sessionId;
}
