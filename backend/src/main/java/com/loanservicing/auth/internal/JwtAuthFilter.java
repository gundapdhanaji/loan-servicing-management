package com.loanservicing.auth.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanservicing.auth.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once for every request, before any controller - reads the token the LoanLinq way.
 *
 * The LoanLinq axios interceptor (src/axios/index.js) sends on every request:
 *   jwt:  <token>                               <- from localStorage "AUTH-TOKEN"
 *   user: {"email":"...","userId":"..."}        <- from localStorage
 *
 * This filter:
 *   1. reads the token from the "jwt" header
 *      (also accepts "Authorization: Bearer <token>" so Postman/curl users are not stuck)
 *   2. checks the signature and the 24h expiry
 *   3. if a "user" header is sent, its userId must match the token (stops a stale/mixed-up session)
 *   4. tells Spring Security who the user is
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    JwtAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            jwtService.parse(token)
                    .filter(user -> userHeaderMatches(request, user))
                    .ifPresent(this::authenticate);
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String jwt = request.getHeader("jwt");
        if (jwt != null && !jwt.isBlank()) {
            return jwt.trim();
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return null;
    }

    /** LoanLinq also sends a "user" header. If present, it must belong to the same user as the token. */
    private boolean userHeaderMatches(HttpServletRequest request, AuthenticatedUser user) {
        String header = request.getHeader("user");
        if (header == null || header.isBlank()) {
            return true;
        }
        try {
            JsonNode node = objectMapper.readTree(header);
            JsonNode userId = node.get("userId");
            if (userId == null || userId.isNull() || userId.asText().isBlank() || "null".equals(userId.asText())) {
                return true; // front-end did not know the id yet
            }
            boolean matches = userId.asText().equals(String.valueOf(user.userId()));
            if (!matches) {
                log.warn("Rejected token: 'user' header userId {} does not match token user {}",
                        userId.asText(), user.userId());
            }
            return matches;
        } catch (IOException e) {
            return true; // unreadable header - rely on the token alone
        }
    }

    private void authenticate(AuthenticatedUser user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
