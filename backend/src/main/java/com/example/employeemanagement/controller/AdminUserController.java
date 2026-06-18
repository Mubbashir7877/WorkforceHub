package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.CreateUserRequest;
import com.example.employeemanagement.dto.LinkEmployeeRequest;
import com.example.employeemanagement.dto.UpdateUserRolesRequest;
import com.example.employeemanagement.dto.UserResponse;
import com.example.employeemanagement.security.UserPrincipal;
import com.example.employeemanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SYSTEM_ADMIN-only user account management. Also gated at the request-matcher
 * level in {@code SecurityConfig} ("/api/v1/admin/**"); the method-level
 * {@code @PreAuthorize} here is a second, independent enforcement point.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<UserResponse> enableUser(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.setEnabled(id, true, principal.getId()));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<UserResponse> disableUser(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.setEnabled(id, false, principal.getId()));
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<UserResponse> updateRoles(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(userService.updateRoles(id, request));
    }

    @PutMapping("/{id}/employee")
    public ResponseEntity<UserResponse> linkEmployee(
            @PathVariable Long id, @Valid @RequestBody LinkEmployeeRequest request) {
        return ResponseEntity.ok(userService.linkEmployee(id, request));
    }

    @PostMapping("/{id}/revoke-tokens")
    public ResponseEntity<Void> revokeTokens(@PathVariable Long id) {
        userService.revokeActiveTokens(id);
        return ResponseEntity.noContent().build();
    }
}
