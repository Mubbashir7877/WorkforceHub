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
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.repository.RoleRepository;
import com.example.employeemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    private Role employeeRole;
    private Role systemAdminRole;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, employeeRepository, refreshTokenService, passwordEncoder);
        employeeRole = new Role(RoleName.EMPLOYEE);
        systemAdminRole = new Role(RoleName.SYSTEM_ADMIN);
    }

    @Test
    void getUser_unknownId_throwsResourceNotFoundException() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createUser_duplicateEmail_throwsDuplicateEmailException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("taken@example.com");
        request.setPassword("password123");
        request.setRoles(Set.of("EMPLOYEE"));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_unknownRole_throwsUnknownRoleException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setRoles(Set.of("NOT_A_ROLE"));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UnknownRoleException.class);
    }

    @Test
    void createUser_employeeAlreadyLinked_throwsEmployeeAlreadyLinkedException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setRoles(Set.of("EMPLOYEE"));
        request.setEmployeeId(5L);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(new Employee(5L, "A", "B", "a@b.com")));
        when(userRepository.existsByEmployeeId(5L)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(EmployeeAlreadyLinkedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_validRequest_encodesPasswordAndAssignsRoles() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("New@Example.com");
        request.setPassword("password123");
        request.setRoles(Set.of("EMPLOYEE"));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(request);

        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.roles()).containsExactly("EMPLOYEE");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void setEnabled_disabling_revokesActiveRefreshTokens() {
        User user = new User("user@example.com", "hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.setEnabled(1L, false, 2L);

        assertThat(user.isEnabled()).isFalse();
        verify(refreshTokenService).revokeAllForUser(user);
    }

    @Test
    void setEnabled_enabling_doesNotRevokeTokens() {
        User user = new User("user@example.com", "hash");
        user.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.setEnabled(1L, true, 2L);

        assertThat(user.isEnabled()).isTrue();
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void setEnabled_disablingOwnAccount_throwsSelfDisableException() {
        assertThatThrownBy(() -> userService.setEnabled(1L, false, 1L))
                .isInstanceOf(SelfDisableException.class);
        verify(userRepository, never()).findById(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void setEnabled_enablingOwnAccount_succeeds() {
        User user = new User("user@example.com", "hash");
        user.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.setEnabled(1L, true, 1L);

        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void updateRoles_removingLastSystemAdmin_throwsLastAdminRoleRemovalException() {
        User user = new User("admin@example.com", "hash");
        user.setRoles(Set.of(systemAdminRole));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(userRepository.countByRolesContaining(RoleName.SYSTEM_ADMIN)).thenReturn(1L);

        UpdateUserRolesRequest request = new UpdateUserRolesRequest();
        request.setRoles(Set.of("EMPLOYEE"));

        assertThatThrownBy(() -> userService.updateRoles(1L, request))
                .isInstanceOf(LastAdminRoleRemovalException.class);
        assertThat(user.hasRole(RoleName.SYSTEM_ADMIN)).isTrue();
    }

    @Test
    void updateRoles_removingSystemAdminWhenAnotherAdminExists_succeeds() {
        User user = new User("admin@example.com", "hash");
        user.setRoles(Set.of(systemAdminRole));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(userRepository.countByRolesContaining(RoleName.SYSTEM_ADMIN)).thenReturn(2L);

        UpdateUserRolesRequest request = new UpdateUserRolesRequest();
        request.setRoles(Set.of("EMPLOYEE"));

        UserResponse response = userService.updateRoles(1L, request);

        assertThat(response.roles()).containsExactly("EMPLOYEE");
    }

    @Test
    void linkEmployee_alreadyLinkedEmployee_throwsEmployeeAlreadyLinkedException() {
        User user = new User("user@example.com", "hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(new Employee(5L, "A", "B", "a@b.com")));
        when(userRepository.existsByEmployeeId(5L)).thenReturn(true);

        LinkEmployeeRequest request = new LinkEmployeeRequest();
        request.setEmployeeId(5L);

        assertThatThrownBy(() -> userService.linkEmployee(1L, request))
                .isInstanceOf(EmployeeAlreadyLinkedException.class);
    }

    @Test
    void linkEmployee_validRequest_setsEmployeeOnUser() {
        User user = new User("user@example.com", "hash");
        Employee employee = new Employee(5L, "A", "B", "a@b.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmployeeId(5L)).thenReturn(false);

        LinkEmployeeRequest request = new LinkEmployeeRequest();
        request.setEmployeeId(5L);

        UserResponse response = userService.linkEmployee(1L, request);

        assertThat(response.linkedEmployee()).isNotNull();
        assertThat(response.linkedEmployee().email()).isEqualTo("a@b.com");
    }

    @Test
    void revokeActiveTokens_unknownUser_throwsResourceNotFoundException() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.revokeActiveTokens(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
