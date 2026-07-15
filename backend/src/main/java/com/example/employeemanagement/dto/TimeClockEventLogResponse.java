package com.example.employeemanagement.dto;

import java.time.Instant;

public record TimeClockEventLogResponse(
        Long id,
        String eventId,
        String eventType,
        Long employeeId,
        String employeeEmail,
        String employeeFullName,
        Long userId,
        String userEmail,
        Long sessionId,
        Instant eventTime,
        Instant consumedAt,
        String sourceTopic,
        String message,
        String metadataJson
) {}
