package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.EmployeeDto;
import com.example.employeemanagement.dto.EmployeeSelfUpdateRequest;
import com.example.employeemanagement.security.UserPrincipal;
import com.example.employeemanagement.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    // MANAGER is granted full read access temporarily because no manager/team
    // hierarchy exists yet. Narrow this to direct reports once it does.
    @PreAuthorize("hasAnyRole('HR_ADMIN','SYSTEM_ADMIN','MANAGER')")
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeDto> getOwnEmployee(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(employeeService.getOwnEmployee(principal.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<EmployeeDto> updateOwnEmployee(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmployeeSelfUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updateOwnEmployee(principal.getId(), request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_ADMIN','SYSTEM_ADMIN','MANAGER') or @employeeAuthorizationService.isSelf(authentication, #id)")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody EmployeeDto employeeDto) {
        EmployeeDto created = employeeService.createEmployee(employeeDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<EmployeeDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDto employeeDto) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, employeeDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<Void> deactivateEmployee(@PathVariable Long id) {
        employeeService.deactivateEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
