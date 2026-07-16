package com.example.employeemanagement.dto;

import java.time.Instant;
import java.util.List;

public record MessageResponse(
        Long id,
        String role,
        String content,
        Boolean grounded,
        String modelName,
        Instant createdAt,
        List<SourceReferenceResponse> sources
) {}
