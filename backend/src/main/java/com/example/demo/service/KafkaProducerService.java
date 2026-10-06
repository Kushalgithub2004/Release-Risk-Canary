package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "release-events";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishReleaseEvent(Long releaseId, String serviceName, String status, String message) {
        try {
            Map<String, Object> event = Map.of(
                "releaseId", releaseId,
                "serviceName", serviceName,
                "status", status,
                "message", message,
                "timestamp", System.currentTimeMillis()
            );
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, String.valueOf(releaseId), payload);
            log.info("📢 Published Kafka Event to [{}]: {}", TOPIC, payload);
        } catch (Exception e) {
            log.error("Failed to publish Kafka event for release {}", releaseId, e);
        }
    }
}
