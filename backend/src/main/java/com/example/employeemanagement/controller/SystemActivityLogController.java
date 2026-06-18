package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.SystemActivityLogResponse;
import com.example.employeemanagement.service.SystemActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/activity-logs")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class SystemActivityLogController {

    private final SystemActivityLogService systemActivityLogService;

    @GetMapping
    public ResponseEntity<Page<SystemActivityLogResponse>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String eventType) {
        return ResponseEntity.ok(systemActivityLogService.getLogs(page, size, eventType));
    }
}
