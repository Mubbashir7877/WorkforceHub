package com.example.employeemanagement.dto;

// Deliberately excludes the API key, base URL, and any other secret/credential —
// safe operational status only.
public record AiStatusResponse(
        boolean enabled,
        boolean providerConfigured,
        String model,
        boolean knowledgeBaseAvailable,
        long activePolicyDocuments
) {}
