package com.example.employeemanagement.kafka;

import com.example.employeemanagement.service.TimeClockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeClockEventConsumer {

    @Value("${app.kafka.timeclock-events-topic}")
    private String topic;

    private final TimeClockService timeClockService;

    @KafkaListener(
        topics  = "${app.kafka.timeclock-events-topic}",
        groupId = "${app.kafka.timeclock-consumer-group}"
    )
    public void consume(TimeClockEvent event) {
        if (event == null || event.getEventId() == null || event.getEventType() == null) {
            log.warn("Received malformed or null TimeClockEvent — skipping");
            return;
        }
        log.info("Consuming {} event [{}] for employee {} from topic {}",
                event.getEventType(), event.getEventId(), event.getEmployeeId(), topic);
        try {
            timeClockService.saveEventLog(event, topic);
        } catch (Exception ex) {
            log.error("Failed to persist time clock event log for event [{}]: {}",
                    event.getEventId(), ex.getMessage());
        }
    }
}
