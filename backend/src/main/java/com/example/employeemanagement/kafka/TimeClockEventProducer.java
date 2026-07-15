package com.example.employeemanagement.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeClockEventProducer {

    @Value("${app.kafka.timeclock-events-topic}")
    private String topic;

    private final KafkaTemplate<String, TimeClockEvent> kafkaTemplate;

    /**
     * Publishes a time clock event to Kafka.
     * If Kafka is temporarily unavailable the error is logged and control returns
     * normally — the time clock session change has already committed to the DB.
     * Production-grade delivery guarantees require the transactional outbox pattern.
     */
    public void publish(TimeClockEvent event) {
        try {
            kafkaTemplate.send(topic, event.getEventId(), event);
            log.info("Published {} event [{}] for employee {} user {}",
                    event.getEventType(), event.getEventId(),
                    event.getEmployeeId(), event.getUserEmail());
        } catch (Exception ex) {
            log.error("Failed to publish {} event [{}] for employee {} — " +
                            "the time clock operation succeeded but the event was not delivered: {}",
                    event.getEventType(), event.getEventId(),
                    event.getEmployeeId(), ex.getMessage());
        }
    }
}
