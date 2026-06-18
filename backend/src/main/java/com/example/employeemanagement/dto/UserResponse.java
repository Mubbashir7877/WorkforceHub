package com.example.employeemanagement.dto;

import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        boolean enabled,
        Set<String> roles,
        EmployeeSummaryDto linkedEmployee
) {}
