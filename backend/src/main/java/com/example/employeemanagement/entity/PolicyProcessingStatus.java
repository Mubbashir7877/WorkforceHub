package com.example.employeemanagement.entity;

/**
 * DRAFT (metadata only, no file yet) → UPLOADED (file stored) → PROCESSING
 * (extraction/chunking/embedding in progress) → READY (indexed, eligible for
 * /activate) or FAILED. INACTIVE is set on deactivation, after which its chunks
 * are removed from the vector store — reactivating requires reprocessing.
 */
public enum PolicyProcessingStatus {
    DRAFT,
    UPLOADED,
    PROCESSING,
    READY,
    FAILED,
    INACTIVE
}
