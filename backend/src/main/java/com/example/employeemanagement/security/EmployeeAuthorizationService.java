package com.example.employeemanagement.security;

import com.example.employeemanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ownership checks used from {@code @PreAuthorize} expressions. Queries the
 * database directly rather than navigating the lazy User-&gt;Employee association,
 * since method-security advice runs outside of any transaction the controller
 * method would otherwise open (open-in-view is disabled for this project).
 */
@Service
@RequiredArgsConstructor
public class EmployeeAuthorizationService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean isSelf(Authentication authentication, Long employeeId) {
        if (authentication == null || employeeId == null) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            return false;
        }
        return userRepository.existsByIdAndEmployeeId(userPrincipal.getId(), employeeId);
    }
}
