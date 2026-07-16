package com.example.employeemanagement.dto;

import java.time.Instant;

public record ConversationSummaryResponse(
        Long id,
        Instant createdAt,
        Instant updatedAt,
        long messageCount
) {}
