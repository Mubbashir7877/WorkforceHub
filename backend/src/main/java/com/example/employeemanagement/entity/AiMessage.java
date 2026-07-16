package com.example.employeemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Never stores hidden provider reasoning/chain-of-thought, API keys, raw
 * Authorization headers, or access/refresh tokens — only the visible
 * question/answer text plus grounding metadata, for auditing.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "ai_messages",
    indexes = {
        @Index(name = "idx_aim_conversation_id", columnList = "conversation_id")
    }
)
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private AiMessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column
    private Boolean grounded;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
