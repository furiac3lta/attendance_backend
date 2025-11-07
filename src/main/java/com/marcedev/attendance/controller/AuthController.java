package com.marcedev.attendance.controller;

import com.marcedev.attendance.security.model.AuthRequest;
import com.marcedev.attendance.security.model.AuthResponse;
import com.marcedev.attendance.security.model.RegisterRequest;
import com.marcedev.attendance.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // ✅ Asegurate que esté esto
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response); // ✅ Devuelve JSON correcto
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response); // ✅ También JSON
    }
}
