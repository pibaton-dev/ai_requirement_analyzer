package com.requirementanalyzer.backend.auth;

import com.requirementanalyzer.backend.auth.AuthDtos.UpdateProfileRequest;
import com.requirementanalyzer.backend.auth.AuthDtos.UserProfile;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public UserProfile getProfile(@AuthenticationPrincipal UUID userId) {
        return authService.getProfile(userId);
    }

    @PatchMapping
    public UserProfile updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return authService.updateProfile(userId, request);
    }
}