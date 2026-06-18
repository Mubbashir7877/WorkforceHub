package com.example.employeemanagement.dto;

public record EmployeeSummaryDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean active
) {}
