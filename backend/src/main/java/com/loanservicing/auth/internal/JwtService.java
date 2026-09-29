package com.loanservicing.auth.internal;

import com.loanservicing.auth.AuthenticatedUser;
import com.loanservicing.auth.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Creates and reads JWT tokens - same approach as LoanLinq:
 * token returned by /api/v1/auth/get_auth_token, valid 24 hours, sent back in the "jwt" header.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    JwtService(@Value("${app.jwt.secret}") String secret,
               @Value("${app.jwt.expiration}") long expirationMillis) {
        if (secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    String createToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt(now)))
                .signWith(key)
                .compact();
    }

    Instant expiresAt(Instant issuedAt) {
        return issuedAt.plus(expirationMillis, ChronoUnit.MILLIS);
    }

    long expirationMillis() {
        return expirationMillis;
    }

    /** Returns the user if the token is valid (right signature, not expired). */
    Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
