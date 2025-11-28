package org.example.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.UserEventInputRequest;
import org.example.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

class KafkaEventConsumerTest {
    private AnalyticsService analyticsService;
    private ObjectMapper mapper;
    private KafkaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        analyticsService = mock(AnalyticsService.class);
        mapper = mock(ObjectMapper.class);
        consumer = new KafkaEventConsumer(analyticsService, mapper);
    }

    @Test
    void listen_shouldProcessValidMessage() throws Exception {
        String msg = "{}";
        UserEventInputRequest req = mock(UserEventInputRequest.class);
        when(mapper.readValue(msg, UserEventInputRequest.class)).thenReturn(req);
        consumer.listen(msg);
        verify(analyticsService).process(req);
    }

    @Test
    void listen_shouldHandleException() throws Exception {
        String msg = "bad_json";
        when(mapper.readValue(msg, UserEventInputRequest.class)).thenThrow(new RuntimeException("fail"));
        consumer.listen(msg);
        // Should not throw
    }
}

