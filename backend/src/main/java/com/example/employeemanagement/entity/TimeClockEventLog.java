package com.example.employeemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "time_clock_event_logs",
    uniqueConstraints = @UniqueConstraint(name = "uk_tcel_event_id", columnNames = "event_id"),
    indexes = {
        @Index(name = "idx_tcel_event_time",  columnList = "event_time"),
        @Index(name = "idx_tcel_employee_id", columnList = "employee_id"),
        @Index(name = "idx_tcel_user_email",  columnList = "user_email"),
        @Index(name = "idx_tcel_event_type",  columnList = "event_type"),
        @Index(name = "idx_tcel_session_id",  columnList = "session_id")
    }
)
public class TimeClockEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "employee_email", length = 255)
    private String employeeEmail;

    @Column(name = "employee_full_name", length = 255)
    private String employeeFullName;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    @Column(name = "source_topic", length = 255)
    private String sourceTopic;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
