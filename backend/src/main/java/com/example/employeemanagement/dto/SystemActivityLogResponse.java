package com.example.employeemanagement.dto;

import java.time.Instant;

public record SystemActivityLogResponse(
        Long id,
        String eventId,
        String eventType,
        String entityType,
        Long entityId,
        Long actorUserId,
        String actorEmail,
        String actorRoles,
        String message,
        Instant occurredAt,
        Instant consumedAt,
        String sourceTopic
) {}
