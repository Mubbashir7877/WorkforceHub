package com.example.employeemanagement.auth;

import com.example.employeemanagement.entity.RefreshToken;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.exception.InvalidRefreshTokenException;
import com.example.employeemanagement.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh tokens are opaque random strings, never JWTs: only their SHA-256 hash is
 * stored, so a leaked database row cannot be replayed, and revocation/rotation
 * always requires a fresh lookup rather than trusting a self-contained token.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();
        RefreshToken entity = new RefreshToken(
                user,
                hash(rawToken),
                LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs))
        );
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /** Validates the given token, revokes it, and issues a replacement (rotation). */
    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is unknown or malformed."));

        if (!existing.isActive()) {
            throw new InvalidRefreshTokenException("Refresh token has expired or already been used.");
        }

        existing.setRevoked(true);
        User user = existing.getUser();
        String newRawToken = issue(user);
        return new RotationResult(user, newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is unknown or malformed."));
        existing.setRevoked(true);
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.findAllByUserAndRevokedFalse(user)
                .forEach(token -> token.setRevoked(true));
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record RotationResult(User user, String rawToken) {}
}
