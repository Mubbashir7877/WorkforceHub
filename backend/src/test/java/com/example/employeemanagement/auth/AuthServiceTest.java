package com.example.employeemanagement.auth;

import com.example.employeemanagement.dto.AuthenticationResponse;
import com.example.employeemanagement.dto.LoginRequest;
import com.example.employeemanagement.entity.Role;
import com.example.employeemanagement.entity.RoleName;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.exception.DisabledAccountException;
import com.example.employeemanagement.security.JwtService;
import com.example.employeemanagement.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtService, refreshTokenService);
        user = new User("user@example.com", "hash");
        user.setRoles(Set.of(new Role(RoleName.EMPLOYEE)));
    }

    @Test
    void login_validCredentials_returnsAuthenticationResponse() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.issue(user)).thenReturn("refresh-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        AuthenticationResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.user().email()).isEqualTo("user@example.com");
    }

    @Test
    void refresh_validToken_returnsNewAuthenticationResponse() {
        when(refreshTokenService.rotate("old-token"))
                .thenReturn(new RefreshTokenService.RotationResult(user, "new-refresh-token"));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthenticationResponse response = authService.refresh("old-token");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refresh_disabledAccount_throwsDisabledAccountException() {
        user.setEnabled(false);
        when(refreshTokenService.rotate("old-token"))
                .thenReturn(new RefreshTokenService.RotationResult(user, "new-refresh-token"));

        assertThatThrownBy(() -> authService.refresh("old-token"))
                .isInstanceOf(DisabledAccountException.class);
    }

    @Test
    void logout_revokesGivenRefreshToken() {
        authService.logout("some-token");

        verify(refreshTokenService).revoke("some-token");
    }
}
