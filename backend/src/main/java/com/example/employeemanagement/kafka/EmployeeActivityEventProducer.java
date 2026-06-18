package com.example.employeemanagement.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeActivityEventProducer {

    @Value("${app.kafka.employee-events-topic}")
    private String topic;

    private final KafkaTemplate<String, EmployeeActivityEvent> kafkaTemplate;

    /**
     * Publishes an employee activity event to Kafka.
     * If Kafka is temporarily unavailable the error is logged and control returns
     * normally — the employee operation that triggered this event has already
     * committed successfully.  Production-grade delivery guarantees require the
     * transactional outbox pattern (documented in README).
     */
    public void publish(EmployeeActivityEvent event) {
        try {
            kafkaTemplate.send(topic, event.getEventId(), event);
            log.info("Published {} event [{}] for employee {} by {}",
                    event.getEventType(), event.getEventId(),
                    event.getEmployeeId(), event.getActorEmail());
        } catch (Exception ex) {
            log.error("Failed to publish {} event [{}] for employee {} — " +
                            "the employee operation succeeded but the activity event was not delivered: {}",
                    event.getEventType(), event.getEventId(),
                    event.getEmployeeId(), ex.getMessage());
        }
    }
}
