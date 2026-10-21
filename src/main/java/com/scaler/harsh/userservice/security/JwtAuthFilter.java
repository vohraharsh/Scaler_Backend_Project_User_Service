package com.scaler.harsh.userservice.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Closes the "hit the service directly on its own port" gap — Kong is no longer the only
 * checkpoint. Public paths here are deliberately kept in sync with kong.yml's route split for
 * this service (user-register-route, user-password-route are public; user-protected-route is
 * JWT-gated), plus /users/verify-credentials, which Kong never routes at all since Auth Service
 * calls it directly, server-to-server, before a JWT exists.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final SecretKey key;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (isPublic(request.getMethod(), request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            reject(response, "Missing or malformed Authorization header");
            return;
        }

        String token = header.substring(7);
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            reject(response, "Token expired");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            reject(response, "Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublic(String method, String path) {
        if ("POST".equalsIgnoreCase(method) && "/users".equals(path)) {
            return true; // registration
        }
        if ("POST".equalsIgnoreCase(method) && "/users/forgot-password".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/users/reset-password".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/users/verify-credentials".equals(path)) {
            return true; // internal, called by Auth Service before a JWT exists
        }
        return false;
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}