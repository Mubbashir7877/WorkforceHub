package com.example.employeemanagement.security;

import com.example.employeemanagement.auth.RefreshTokenService;
import com.example.employeemanagement.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Issues and validates signed JWT access tokens. Refresh tokens are intentionally
 * NOT JWTs - see {@link RefreshTokenService} - so that revocation always requires a
 * database lookup rather than trusting a self-contained token.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTokenExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);
        List<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("roles", roleNames)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    /**
     * Verifies signature, issuer, and expiration. Throws {@link io.jsonwebtoken.JwtException}
     * (or IllegalArgumentException for a blank token) on any failure.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return parseAndValidate(token).getSubject();
    }
}
