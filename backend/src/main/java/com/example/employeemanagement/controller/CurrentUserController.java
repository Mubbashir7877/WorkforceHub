package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.UserResponse;
import com.example.employeemanagement.security.UserPrincipal;
import com.example.employeemanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The principal attached by {@code JwtAuthenticationFilter} was loaded in its own,
 * already-closed transaction, so its lazy {@code employee} association cannot be
 * read directly here. We re-fetch by id inside a fresh, open transaction instead.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class CurrentUserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getId()));
    }
}
