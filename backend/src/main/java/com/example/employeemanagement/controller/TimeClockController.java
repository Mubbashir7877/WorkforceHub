package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.*;
import com.example.employeemanagement.service.TimeClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST endpoints for the employee time clock feature.
 *
 * Employee-facing endpoints (/status, /clock-in, /clock-out, /my-sessions) are
 * accessible to any authenticated user who has a linked employee profile.
 *
 * The /events endpoint is restricted to MANAGER, HR_ADMIN, and SYSTEM_ADMIN.
 * NOTE: MANAGER currently sees all time clock events. In a future phase this
 * should be narrowed to direct reports only once team hierarchy is implemented.
 */
@RestController
@RequestMapping("/api/v1/time-clock")
@RequiredArgsConstructor
public class TimeClockController {

    private final TimeClockService timeClockService;

    @GetMapping("/status")
    public ResponseEntity<TimeClockStatusResponse> getStatus() {
        return ResponseEntity.ok(timeClockService.getStatus());
    }

    @PostMapping("/clock-in")
    public ResponseEntity<ClockInResponse> clockIn() {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeClockService.clockIn());
    }

    @PostMapping("/clock-out")
    public ResponseEntity<ClockOutResponse> clockOut() {
        return ResponseEntity.ok(timeClockService.clockOut());
    }

    @GetMapping("/my-sessions")
    public ResponseEntity<Page<TimeClockSessionResponse>> getMySessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(timeClockService.getMySessions(page, size));
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<Page<TimeClockEventLogResponse>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String userEmail) {

        return ResponseEntity.ok(
                timeClockService.getEventLogs(page, size, employeeId, eventType, startDate, endDate, userEmail));
    }
}
