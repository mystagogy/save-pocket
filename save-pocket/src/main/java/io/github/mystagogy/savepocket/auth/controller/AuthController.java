package io.github.mystagogy.savepocket.auth.controller;

import io.github.mystagogy.savepocket.auth.dto.AuthUserResponse;
import io.github.mystagogy.savepocket.auth.dto.ChangePasswordRequest;
import io.github.mystagogy.savepocket.auth.dto.LoginRequest;
import io.github.mystagogy.savepocket.auth.dto.SignupRequest;
import io.github.mystagogy.savepocket.auth.dto.UpdateNicknameRequest;
import io.github.mystagogy.savepocket.auth.service.AuthService;
import io.github.mystagogy.savepocket.common.security.CurrentUserProvider;
import io.github.mystagogy.savepocket.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthUserResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthUserResponse user = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthUserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AuthUserResponse user = authService.login(request, servletRequest, servletResponse);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        authService.logout(servletRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> me(HttpServletRequest servletRequest) {
        AuthUserResponse user = authService.me(servletRequest);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<AuthUserResponse>> updateNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        AuthUserResponse user = authService.updateNickname(userId, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        authService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
