package com.practice.knowheart.controller;

import com.practice.knowheart.dto.AuthResponse;
import com.practice.knowheart.dto.LoginRequest;
import com.practice.knowheart.dto.RegisterRequest;
import com.practice.knowheart.dto.UserCenterResponse;
import com.practice.knowheart.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowheart/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(extractToken(authorization));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserCenterResponse> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(authService.getUserCenter(extractToken(authorization)));
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return authorization.trim();
    }
}
