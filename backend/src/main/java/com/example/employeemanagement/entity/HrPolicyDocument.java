package com.example.employeemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "hr_policy_documents",
    indexes = {
        @Index(name = "idx_hpd_active",            columnList = "active"),
        @Index(name = "idx_hpd_category",           columnList = "category"),
        @Index(name = "idx_hpd_processing_status",   columnList = "processing_status")
    }
)
public class HrPolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PolicyCategory category;

    @Column(length = 50)
    private String version;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    // Soft-deactivation flag. Documents are never hard-deleted.
    @Column(nullable = false)
    private boolean active = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "processing_status", nullable = false, length = 12)
    private PolicyProcessingStatus processingStatus = PolicyProcessingStatus.DRAFT;

    // Number of chunks currently indexed in the vector store for this document.
    // Used to compute deterministic chunk ids ("doc-{id}-chunk-{i}") so
    // re-processing can delete-then-replace without leaving stray/duplicate entries.
    @Column(name = "chunk_count", nullable = false)
    private int chunkCount = 0;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "storage_type", length = 10)
    private StorageType storageType;

    @Column(name = "storage_location", length = 500)
    private String storageLocation;

    @Column(name = "uploaded_by_user_id")
    private Long uploadedByUserId;

    @Column(name = "uploaded_by_email", length = 255)
    private String uploadedByEmail;

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
