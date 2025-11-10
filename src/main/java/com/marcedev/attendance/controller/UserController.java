package com.marcedev.attendance.controller;

import com.marcedev.attendance.dto.UserDTO;
import com.marcedev.attendance.entities.User;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.repository.CourseRepository;
import com.marcedev.attendance.repository.UserRepository;
import com.marcedev.attendance.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    // ✅ Listar usuarios mostrando organización + cursos
    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

            List<User> users;

            // SUPER_ADMIN → ve todos
            if (currentUser.getRole() == Rol.SUPER_ADMIN) {
                users = userService.findAll();
            }
            // ADMIN → solo los de su organización
            else if (currentUser.getRole() == Rol.ADMIN) {
                if (currentUser.getOrganization() == null) {
                    return ResponseEntity.badRequest()
                            .body("⚠️ Este admin no tiene organización asignada.");
                }
                Long orgId = currentUser.getOrganization().getId();
                users = userRepository.findByOrganizationId(orgId);
            }
            else {
                return ResponseEntity.status(403)
                        .body("🚫 No tiene permisos para listar usuarios.");
            }

            // Convertimos a DTO para front
            var result = users.stream().map(u -> {
                var dto = new UserDTO();
                dto.setId(u.getId());
                dto.setFullName(u.getFullName());
                dto.setEmail(u.getEmail());
                dto.setRole(u.getRole().name());
                dto.setOrganizationName(u.getOrganization() != null ? u.getOrganization().getName() : null);
                dto.setCourses(
                        u.getCourses() != null
                                ? u.getCourses().stream().map(c -> c.getName()).toList()
                                : List.of()
                );
                return dto;
            }).toList();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("❌ Error al obtener usuarios: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody User user) {
        try {
            Rol currentRole = getCurrentUserRole();

            if (currentRole == Rol.INSTRUCTOR || currentRole == Rol.USER)
                return ResponseEntity.status(403).body("🚫 Sin permisos.");

            if (currentRole == Rol.ADMIN) {
                var me = userService.findByEmail(getAuthenticatedEmail())
                        .orElseThrow();

                user.setOrganization(me.getOrganization());
                if (user.getRole() == Rol.SUPER_ADMIN)
                    return ResponseEntity.status(403).body("🚫 No puede crear SUPER_ADMIN.");
            }

            return ResponseEntity.ok(userService.save(user));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/assign-courses")
    public ResponseEntity<?> assignCourses(@PathVariable Long userId, @RequestBody List<Long> courseIds) {
        try {
            Rol role = getCurrentUserRole();

            if (role != Rol.SUPER_ADMIN && role != Rol.ADMIN && role != Rol.INSTRUCTOR)
                return ResponseEntity.status(403).body("🚫 Sin permisos.");

            return ResponseEntity.ok(userService.assignCourses(userId, courseIds));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ " + e.getMessage());
        }
    }

    private String getAuthenticatedEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Rol getCurrentUserRole() {
        String email = getAuthenticatedEmail();
        return userService.findByEmail(email).orElseThrow().getRole();
    }
    @GetMapping("/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable Rol role) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        List<User> users;

        if (currentUser.getRole() == Rol.SUPER_ADMIN) {
            // Superadmin ve todos
            users = userRepository.findByRole(role);
        } else if (currentUser.getRole() == Rol.ADMIN) {
            // Admin solo ve instructores de su organización
            if (currentUser.getOrganization() == null) {
                return ResponseEntity.badRequest()
                        .body("⚠️ Este admin no tiene organización asignada.");
            }
            users = userRepository.findByRoleAndOrganizationId(role, currentUser.getOrganization().getId());
        } else {
            return ResponseEntity.status(403)
                    .body("🚫 No tienes permisos para ver instructores.");
        }

        // Evitar referencias innecesarias en JSON
        users.forEach(u -> {
            if (u.getCourses() != null) {
                u.getCourses().forEach(c -> c.setInstructor(null));
            }
        });

        return ResponseEntity.ok(users);
    }

}
