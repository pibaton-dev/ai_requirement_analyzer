package com.requirementanalyzer.backend.auth;

import com.requirementanalyzer.backend.auth.AuthDtos.AuthResponse;
import com.requirementanalyzer.backend.auth.AuthDtos.AuthResult;
import com.requirementanalyzer.backend.auth.AuthDtos.LoginRequest;
import com.requirementanalyzer.backend.auth.AuthDtos.RegisterRequest;
import com.requirementanalyzer.backend.auth.AuthDtos.UpdateProfileRequest;
import com.requirementanalyzer.backend.auth.AuthDtos.UserProfile;
import com.requirementanalyzer.backend.common.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;
    private final Duration refreshTokenTtl;

    public AuthService(
            UserAccountRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenHasher tokenHasher,
            @Value("${security.refresh-token-ttl}") Duration refreshTokenTtl
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenHasher = tokenHasher;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new DomainException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email already exists.");
        }
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Required USER role is missing"));
        UserAccount user = userRepository.save(new UserAccount(
                email,
                request.displayName().trim(),
                passwordEncoder.encode(request.password()),
                userRole
        ));
        return createSession(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        UserAccount user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect."));
        return createSession(user);
    }

    @Transactional
    public AuthResult refresh(String rawToken) {
        Instant now = Instant.now();
        RefreshToken oldToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokenHasher.hash(rawToken), now)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "The refresh session is invalid or expired."));
        UserAccount user = oldToken.getUser();
        if (user.getStatus() != AccountStatus.ACTIVE) {
            oldToken.revoke(now);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "ACCOUNT_INACTIVE", "This account is not active.");
        }
        oldToken.revoke(now);
        return createSession(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHasher.hash(rawToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(UUID userId) {
        return toProfile(findUser(userId));
    }

    @Transactional
    public UserProfile updateProfile(UUID userId, UpdateProfileRequest request) {
        UserAccount user = findUser(userId);
        user.updateDisplayName(request.displayName().trim());
        return toProfile(user);
    }

    private AuthResult createSession(UserAccount user) {
        JwtService.IssuedToken accessToken = jwtService.issue(user);
        String refreshToken = generateRefreshToken();
        refreshTokenRepository.save(new RefreshToken(
                user,
                tokenHasher.hash(refreshToken),
                Instant.now().plus(refreshTokenTtl)
        ));
        return new AuthResult(
                new AuthResponse(accessToken.value(), accessToken.expiresAt(), toProfile(user)),
                refreshToken
        );
    }

    private UserAccount findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."));
    }

    private UserProfile toProfile(UserAccount user) {
        return new UserProfile(user.getId(), user.getEmail(), user.getDisplayName(), user.getStatus().name());
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}