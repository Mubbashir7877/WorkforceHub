package com.example.employeemanagement.dto;

import java.time.Instant;
import java.util.List;

public record ChatResponse(
        String answer,
        boolean grounded,
        List<SourceReferenceResponse> sources,
        Long conversationId,
        Instant createdAt
) {}
