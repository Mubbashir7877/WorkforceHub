package com.example.employeemanagement.service;

import com.example.employeemanagement.auth.RefreshTokenService;
import com.example.employeemanagement.dto.CreateUserRequest;
import com.example.employeemanagement.dto.LinkEmployeeRequest;
import com.example.employeemanagement.dto.UpdateUserRolesRequest;
import com.example.employeemanagement.dto.UserResponse;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.entity.Role;
import com.example.employeemanagement.entity.RoleName;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.exception.DuplicateEmailException;
import com.example.employeemanagement.exception.EmployeeAlreadyLinkedException;
import com.example.employeemanagement.exception.LastAdminRoleRemovalException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.exception.SelfDisableException;
import com.example.employeemanagement.exception.UnknownRoleException;
import com.example.employeemanagement.mapper.UserMapper;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.repository.RoleRepository;
import com.example.employeemanagement.repository.UserRepository;
import com.example.employeemanagement.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Backs the SYSTEM_ADMIN-only user administration endpoints. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(UserMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return UserMapper.toResponse(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long id) {
        return UserMapper.toResponse(findUserOrThrow(id));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = CustomUserDetailsService.normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("An account already exists for email: " + email);
        }

        User user = new User(email, passwordEncoder.encode(request.getPassword()));
        user.setRoles(resolveRoles(request.getRoles()));

        if (request.getEmployeeId() != null) {
            user.setEmployee(resolveUnlinkedEmployee(request.getEmployeeId()));
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled, Long actingUserId) {
        if (!enabled && id.equals(actingUserId)) {
            throw new SelfDisableException("You cannot disable your own account.");
        }
        User user = findUserOrThrow(id);
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenService.revokeAllForUser(user);
        }
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateRoles(Long id, UpdateUserRolesRequest request) {
        User user = findUserOrThrow(id);
        Set<Role> newRoles = resolveRoles(request.getRoles());

        boolean losingSystemAdmin = user.hasRole(RoleName.SYSTEM_ADMIN)
                && newRoles.stream().noneMatch(role -> role.getName() == RoleName.SYSTEM_ADMIN);
        if (losingSystemAdmin && userRepository.countByRolesContaining(RoleName.SYSTEM_ADMIN) <= 1) {
            throw new LastAdminRoleRemovalException(
                    "Cannot remove SYSTEM_ADMIN from the last remaining administrator.");
        }

        user.setRoles(newRoles);
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponse linkEmployee(Long id, LinkEmployeeRequest request) {
        User user = findUserOrThrow(id);
        user.setEmployee(resolveUnlinkedEmployee(request.getEmployeeId()));
        return UserMapper.toResponse(user);
    }

    @Transactional
    public void revokeActiveTokens(Long id) {
        refreshTokenService.revokeAllForUser(findUserOrThrow(id));
    }

    private Employee resolveUnlinkedEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        if (userRepository.existsByEmployeeId(employeeId)) {
            throw new EmployeeAlreadyLinkedException(
                    "Employee " + employeeId + " is already linked to a user account.");
        }
        return employee;
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        return roleNames.stream().map(this::resolveRole).collect(Collectors.toSet());
    }

    private Role resolveRole(String roleName) {
        RoleName parsed;
        try {
            parsed = RoleName.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnknownRoleException("Unknown role: " + roleName);
        }
        return roleRepository.findByName(parsed)
                .orElseThrow(() -> new UnknownRoleException("Unknown role: " + roleName));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
