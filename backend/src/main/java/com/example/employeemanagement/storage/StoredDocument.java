package com.example.employeemanagement.storage;

public record StoredDocument(
        String storageLocation,
        String originalFileName,
        String contentType,
        long sizeBytes
) {}
