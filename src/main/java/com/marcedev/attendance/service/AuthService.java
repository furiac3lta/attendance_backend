package com.marcedev.attendance.service;

import com.marcedev.attendance.security.model.AuthRequest;
import com.marcedev.attendance.security.model.AuthResponse;
import com.marcedev.attendance.security.model.RegisterRequest;

/**
 * 🔹 Servicio de autenticación (registro + login)
 * Maneja la creación de usuarios, login y generación de tokens JWT.
 */
public interface AuthService {

    // Registro de usuario (ADMIN, INSTRUCTOR o USER)
    AuthResponse register(RegisterRequest request);

    // Inicio de sesión
    AuthResponse login(AuthRequest request);
}
