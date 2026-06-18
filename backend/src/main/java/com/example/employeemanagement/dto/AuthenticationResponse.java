package com.example.employeemanagement.dto;

public record AuthenticationResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {}
