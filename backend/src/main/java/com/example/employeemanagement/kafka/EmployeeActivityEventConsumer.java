package com.example.employeemanagement.kafka;

import com.example.employeemanagement.service.SystemActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeActivityEventConsumer {

    @Value("${app.kafka.employee-events-topic}")
    private String topic;

    private final SystemActivityLogService systemActivityLogService;

    @KafkaListener(
        topics    = "${app.kafka.employee-events-topic}",
        groupId   = "${app.kafka.consumer-group:${spring.kafka.consumer.group-id}}"
    )
    public void consume(EmployeeActivityEvent event) {
        if (event == null || event.getEventId() == null || event.getEventType() == null) {
            log.warn("Received malformed or null EmployeeActivityEvent — skipping");
            return;
        }
        log.info("Consuming {} event [{}] for employee {} from topic {}",
                event.getEventType(), event.getEventId(), event.getEmployeeId(), topic);
        try {
            systemActivityLogService.save(event, topic);
        } catch (Exception ex) {
            log.error("Failed to persist activity log for event [{}]: {}",
                    event.getEventId(), ex.getMessage());
        }
    }
}
