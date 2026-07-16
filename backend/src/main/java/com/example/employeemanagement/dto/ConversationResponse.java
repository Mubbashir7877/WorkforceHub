package com.example.employeemanagement.dto;

import java.time.Instant;
import java.util.List;

public record ConversationResponse(
        Long id,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages
) {}
