package com.example.employeemanagement.dto;

import java.time.Instant;

public record TimeClockStatusResponse(
        boolean clockedIn,
        Long sessionId,
        Instant clockInTime
) {}
