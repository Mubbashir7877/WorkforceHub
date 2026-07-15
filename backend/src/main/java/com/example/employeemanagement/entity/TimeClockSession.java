package com.example.employeemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "time_clock_sessions",
    indexes = {
        @Index(name = "idx_tcs_employee_status", columnList = "employee_id, status"),
        @Index(name = "idx_tcs_user_id",         columnList = "user_id"),
        @Index(name = "idx_tcs_clock_in_time",   columnList = "clock_in_time")
    }
)
public class TimeClockSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "clock_in_time", nullable = false)
    private Instant clockInTime;

    @Column(name = "clock_out_time")
    private Instant clockOutTime;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private TimeClockStatus status = TimeClockStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
