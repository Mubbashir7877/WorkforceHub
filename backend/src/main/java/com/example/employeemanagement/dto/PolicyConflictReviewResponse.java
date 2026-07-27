package com.example.employeemanagement.dto;

import java.time.Instant;
import java.util.List;

// Advisory only — never persisted, always recomputed on request. A human
// (HR_ADMIN/SYSTEM_ADMIN) always makes the actual activation decision.
public record PolicyConflictReviewResponse(
        Long documentId,
        boolean hasConflicts,
        List<PolicyConflictItemResponse> conflicts,
        Instant reviewedAt
) {}
