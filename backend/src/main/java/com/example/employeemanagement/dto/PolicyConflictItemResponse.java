package com.example.employeemanagement.dto;

// Excerpts are intentionally short, mirroring SourceReferenceResponse — never
// the full chunk or document text.
public record PolicyConflictItemResponse(
        Long conflictingDocumentId,
        String conflictingDocumentTitle,
        String conflictingDocumentCategory,
        String newDocumentExcerpt,
        String conflictingDocumentExcerpt,
        String explanation
) {}
