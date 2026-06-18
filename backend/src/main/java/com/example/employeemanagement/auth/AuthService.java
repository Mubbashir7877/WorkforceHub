package com.example.employeemanagement.auth;

import com.example.employeemanagement.dto.AuthenticationResponse;
import com.example.employeemanagement.dto.LoginRequest;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.exception.DisabledAccountException;
import com.example.employeemanagement.mapper.UserMapper;
import com.example.employeemanagement.security.CustomUserDetailsService;
import com.example.employeemanagement.security.JwtService;
import com.example.employeemanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates login, refresh, and logout. Account-status checks (disabled,
 * locked, bad credentials) for the password-based login flow are enforced by
 * {@link AuthenticationManager} / {@link CustomUserDetailsService} - this class
 * only adds the equivalent check for the refresh flow, which never touches a
 * password.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        String email = CustomUserDetailsService.normalizeEmail(request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
        return buildAuthenticationResponse(user);
    }

    @Transactional
    public AuthenticationResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);
        User user = rotation.user();

        if (!user.isEnabled()) {
            throw new DisabledAccountException("This account has been disabled. Contact an administrator.");
        }

        return new AuthenticationResponse(
                jwtService.generateAccessToken(user),
                rotation.rawToken(),
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                UserMapper.toResponse(user)
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthenticationResponse buildAuthenticationResponse(User user) {
        return new AuthenticationResponse(
                jwtService.generateAccessToken(user),
                refreshTokenService.issue(user),
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                UserMapper.toResponse(user)
        );
    }
}
