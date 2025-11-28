package org.example.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.dto.UserEventInputRequest;
import org.example.enums.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class MockEventGenerator {

    private static final Logger log = LoggerFactory.getLogger(MockEventGenerator.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;
    private final Random random = new Random();

    @Value("${analytics.mock.enabled}")
    private boolean enabled;

    @Value("${analytics.mock.qps:100}")
    private int eventsPerSecond;

    @Value("${analytics.mock.topic:events}")
    private String topic;

    private static final List<String> EVENT_TYPES = List.of(
            "PAGE_VIEW", "CLICK", "ADD_TO_CART", "CHECKOUT", "SEARCH"
    );

    private static final List<String> PAGES = List.of(
            "/home", "/product/123", "/checkout", "/search?q=shoes", "/category/electronics"
    );

    public MockEventGenerator(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
    }

    /**
     * Runs every second and publishes N events based on QPS.
     */
    @Scheduled(fixedRate = 300000)
    public void generateEvents() {
        if (!enabled) return;
        for (int i = 0; i < eventsPerSecond; i++) {
            UserEventInputRequest ev = new UserEventInputRequest();
            ev.setEventTimestamp(Instant.now());
            ev.setUserId(UUID.randomUUID());
            ev.setEventType(EventType.valueOf(EVENT_TYPES.get(random.nextInt(EVENT_TYPES.size()))));
            ev.setPageUrl(PAGES.get(random.nextInt(PAGES.size())));

            try {
                String json = mapper.writeValueAsString(ev);
                kafkaTemplate.send(topic, ev.getUserId().toString(), json);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize mock event", e);
            } catch (Exception e) {
                log.error("Failed to send mock event", e);
            }
        }

        log.info("Generated {} mock events into topic={}", eventsPerSecond, topic);
    }
}
