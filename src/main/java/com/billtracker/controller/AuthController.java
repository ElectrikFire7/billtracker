package com.billtracker.controller;

import com.billtracker.dto.request.GoogleOAuthRequest;
import com.billtracker.dto.response.AuthResponse;
import com.billtracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleOAuthRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }
}
