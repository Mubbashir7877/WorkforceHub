package com.example.employeemanagement.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeClockEvent {

    private String eventId;
    private TimeClockEventType eventType;

    private Long employeeId;
    private String employeeEmail;
    private String employeeFullName;

    private Long userId;
    private String userEmail;

    private Long sessionId;
    private Instant eventTime;
    private String message;

    /** Non-sensitive supplementary data (e.g. clockInTime, clockOutTime, durationMinutes). */
    private Map<String, String> metadata;
}
