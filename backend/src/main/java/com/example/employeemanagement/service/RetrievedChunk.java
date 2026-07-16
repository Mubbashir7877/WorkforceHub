package com.example.employeemanagement.service;

public record RetrievedChunk(
        Long documentId,
        String documentTitle,
        String category,
        String version,
        Integer pageNumber,
        int chunkIndex,
        String effectiveDate,
        String content,
        double score
) {}
