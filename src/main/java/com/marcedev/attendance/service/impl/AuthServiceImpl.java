package com.marcedev.attendance.service.impl;

import com.marcedev.attendance.dto.UserDTO;
import com.marcedev.attendance.entities.Organization;
import com.marcedev.attendance.entities.User;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.repository.UserRepository;
import com.marcedev.attendance.security.jwt.JwtService;
import com.marcedev.attendance.security.model.AuthRequest;
import com.marcedev.attendance.security.model.AuthResponse;
import com.marcedev.attendance.security.model.RegisterRequest;
import com.marcedev.attendance.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ======================================================
    // 🔹 REGISTRO
    // ======================================================
    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        Rol role;
        try {
            role = Rol.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            role = Rol.USER;
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        // Si quien crea tiene organización → asignar organización
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            userRepository.findByEmail(auth.getName()).ifPresent(current -> {
                if (current.getOrganization() != null)
                    user.setOrganization(current.getOrganization());
            });
        }

        userRepository.save(user);

        // ✅ Generar token de forma correcta (UserDetails)
        String token = jwtService.generateToken(
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build()
        );

        UserDTO dto = new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getOrganization() != null ? user.getOrganization().getName() : null,
                user.getCourses() != null ? user.getCourses().stream().map(c -> c.getName()).toList() : List.of(),
                user.getOrganization() != null ? user.getOrganization().getId() : null
        );

        return AuthResponse.builder()
                .token(token)
                .type(user.getRole().name())
                .user(dto)
                .build();
    }

    // ======================================================
    // 🔹 LOGIN
    // ======================================================
    @Override
    public AuthResponse login(AuthRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ Generar token de forma correcta
        String token = jwtService.generateToken(
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build()
        );

        UserDTO dto = new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getOrganization() != null ? user.getOrganization().getName() : null,
                user.getCourses() != null ? user.getCourses().stream().map(c -> c.getName()).toList() : List.of(),
                user.getOrganization() != null ? user.getOrganization().getId() : null
        );

        return AuthResponse.builder()
                .token(token)
                .type(user.getRole().name())
                .user(dto)
                .build();
    }
}
