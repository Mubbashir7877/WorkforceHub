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
    name = "system_activity_logs",
    uniqueConstraints = @UniqueConstraint(name = "uk_sal_event_id", columnNames = "event_id"),
    indexes = {
        @Index(name = "idx_sal_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_sal_event_type",  columnList = "event_type"),
        @Index(name = "idx_sal_actor_email", columnList = "actor_email"),
        @Index(name = "idx_sal_entity",      columnList = "entity_type, entity_id")
    }
)
public class SystemActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    /** Comma-separated role names (e.g. "HR_ADMIN,SYSTEM_ADMIN"). */
    @Column(name = "actor_roles", length = 255)
    private String actorRoles;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    @Column(name = "source_topic", length = 255)
    private String sourceTopic;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
