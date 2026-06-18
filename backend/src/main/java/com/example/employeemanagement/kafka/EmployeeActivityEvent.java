package com.example.employeemanagement.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeActivityEvent {

    private String eventId;
    private EmployeeEventType eventType;
    private String entityType;

    private Long employeeId;
    private String employeeEmail;
    private String employeeFullName;

    private Long actorUserId;
    private String actorEmail;
    private List<String> actorRoles;

    private String message;
    private Instant occurredAt;

    /** Small, non-sensitive supplementary key/value pairs. */
    private Map<String, String> metadata;
}
