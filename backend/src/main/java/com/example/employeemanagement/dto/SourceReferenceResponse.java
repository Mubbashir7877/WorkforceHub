package com.example.employeemanagement.dto;

// excerpt is intentionally short — never the full chunk or document text.
public record SourceReferenceResponse(
        Long documentId,
        String title,
        String category,
        String version,
        Integer pageNumber,
        String excerpt
) {}
