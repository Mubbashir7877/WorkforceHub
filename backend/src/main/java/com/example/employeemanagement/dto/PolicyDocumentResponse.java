package com.example.employeemanagement.dto;

import java.time.Instant;
import java.time.LocalDate;

// Deliberately omits storageLocation — never expose internal storage paths to clients.
public record PolicyDocumentResponse(
        Long id,
        String title,
        String description,
        String category,
        String version,
        LocalDate effectiveDate,
        boolean active,
        String processingStatus,
        int chunkCount,
        String originalFileName,
        String contentType,
        String uploadedByEmail,
        Instant createdAt,
        Instant updatedAt
) {}
