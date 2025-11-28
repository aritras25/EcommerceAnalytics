package org.example.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.example.config.ObjectMapperConfig;
import org.example.dto.UserEventInputRequest;
import org.example.service.AnalyticsService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaEventConsumer {
    private final AnalyticsService analyticsService;
    private final ObjectMapper mapper;

    public KafkaEventConsumer(AnalyticsService analyticsService, ObjectMapper mapper) { this.analyticsService = analyticsService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "user_events", containerFactory = "kafkaListenerContainerFactory")
    public void listen(String message) {
        try {
            UserEventInputRequest ev = mapper.readValue(message, UserEventInputRequest.class);
            log.info("Message consumed");
            analyticsService.process(ev);
            log.info("Message processed");
        } catch (Exception e) {
            log.error("Failed to process message", e);
        }
    }
}
