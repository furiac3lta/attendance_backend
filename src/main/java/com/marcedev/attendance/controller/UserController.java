// src/main/java/com/marcedev/attendance/controller/UserController.java
package com.marcedev.attendance.controller;

import com.marcedev.attendance.dto.UserDTO;
import com.marcedev.attendance.entities.User;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.repository.CourseRepository;
import com.marcedev.attendance.repository.UserRepository;
import com.marcedev.attendance.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    // ==========================================================
    // ✅ LISTAR USUARIOS (PAGINADO)
    // ==========================================================
    @GetMapping
    public ResponseEntity<Page<UserDTO>> getAll(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        Page<User> page;
        if (currentUser.getRole() == Rol.SUPER_ADMIN) {
            page = userRepository.findAll(pageable);
        } else if (currentUser.getRole() == Rol.ADMIN) {
            if (currentUser.getOrganization() == null) {
                return ResponseEntity.badRequest().body(Page.empty());
            }
            Long orgId = currentUser.getOrganization().getId();
            page = userRepository.findByOrganizationId(orgId, pageable);
        } else {
            return ResponseEntity.status(403).body(Page.empty());
        }

        Page<UserDTO> dtoPage = page.map(u -> new UserDTO(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getRole().name(),
                u.getOrganization() != null ? u.getOrganization().getName() : null,
                u.getCourses() != null ? u.getCourses().stream().map(c -> c.getName()).toList() : List.of(),
                u.getOrganization() != null ? u.getOrganization().getId() : null
        ));

        return ResponseEntity.ok(dtoPage);
    }

    // ==========================================================
    // ✅ CREAR USUARIO
    // ==========================================================
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody User user) {
        Rol currentRole = getCurrentUserRole();
        if (currentRole == Rol.INSTRUCTOR || currentRole == Rol.USER)
            return ResponseEntity.status(403).body("🚫 Sin permisos.");

        if (currentRole == Rol.ADMIN) {
            var me = userService.findByEmail(getAuthenticatedEmail()).orElseThrow();
            user.setOrganization(me.getOrganization());
            if (user.getRole() == Rol.SUPER_ADMIN)
                return ResponseEntity.status(403).body("🚫 No puede crear SUPER_ADMIN.");
        }
        return ResponseEntity.ok(userService.save(user));
    }

    // ==========================================================
    // ✅ EDITAR USUARIO (NUEVO – NECESARIO PARA NETLIFY)
    // ==========================================================
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody UserDTO userDTO
    ) {
        UserDTO updated = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updated);
    }

    // ==========================================================
    // ✅ ASIGNAR CURSOS
    // ==========================================================
    @PostMapping("/{userId}/assign-courses")
    public ResponseEntity<?> assignCourses(@PathVariable Long userId, @RequestBody List<Long> courseIds) {
        Rol role = getCurrentUserRole();
        if (role != Rol.SUPER_ADMIN && role != Rol.ADMIN && role != Rol.INSTRUCTOR)
            return ResponseEntity.status(403).body("🚫 Sin permisos.");

        return ResponseEntity.ok(userService.assignCourses(userId, courseIds));
    }

    // ==========================================================
    // ✅ LISTAR POR ROL
    // ==========================================================
    @GetMapping("/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable Rol role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        List<User> users;
        if (currentUser.getRole() == Rol.SUPER_ADMIN) {
            users = userRepository.findByRole(role);
        } else if (currentUser.getRole() == Rol.ADMIN) {
            if (currentUser.getOrganization() == null) {
                return ResponseEntity.badRequest().body("⚠️ Este admin no tiene organización asignada.");
            }
            users = userRepository.findByRoleAndOrganizationId(role, currentUser.getOrganization().getId());
        } else {
            return ResponseEntity.status(403).body("🚫 No tienes permisos para ver instructores.");
        }

        users.forEach(u -> {
            if (u.getCourses() != null) {
                u.getCourses().forEach(c -> c.setInstructor(null));
            }
        });

        return ResponseEntity.ok(users);
    }

    // ==========================================================
    // 🔧 HELPERS INTERNOS
    // ==========================================================
    private String getAuthenticatedEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Rol getCurrentUserRole() {
        String email = getAuthenticatedEmail();
        return userService.findByEmail(email).orElseThrow().getRole();
    }
}
