package com.example.employeemanagement.controller.ai;

import com.example.employeemanagement.dto.AiStatusResponse;
import com.example.employeemanagement.service.AiOperationalStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/ai")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class SystemAiStatusController {

    private final AiOperationalStatusService statusService;

    @GetMapping("/status")
    public ResponseEntity<AiStatusResponse> getStatus() {
        return ResponseEntity.ok(statusService.getStatus());
    }
}
