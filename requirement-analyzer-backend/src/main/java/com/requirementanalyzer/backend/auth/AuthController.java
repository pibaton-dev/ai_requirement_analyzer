package com.requirementanalyzer.backend.auth;

import com.requirementanalyzer.backend.auth.AuthDtos.AuthResponse;
import com.requirementanalyzer.backend.auth.AuthDtos.AuthResult;
import com.requirementanalyzer.backend.auth.AuthDtos.LoginRequest;
import com.requirementanalyzer.backend.auth.AuthDtos.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final boolean secureCookie;
    private final Duration refreshTokenTtl;

    public AuthController(
            AuthService authService,
            @Value("${security.refresh-cookie.secure}") boolean secureCookie,
            @Value("${security.refresh-token-ttl}") Duration refreshTokenTtl
    ) {
        this.authService = authService;
        this.secureCookie = secureCookie;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return sessionResponse(authService.register(request), response, 201);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return sessionResponse(authService.login(request), response, 200);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new com.requirementanalyzer.backend.common.DomainException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "INVALID_REFRESH_TOKEN",
                    "The refresh session is invalid or expired."
            );
        }
        return sessionResponse(authService.refresh(refreshToken), response, 200);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> sessionResponse(AuthResult result, HttpServletResponse response, int status) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), refreshTokenTtl).toString());
        return ResponseEntity.status(status).body(result.response());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }
}