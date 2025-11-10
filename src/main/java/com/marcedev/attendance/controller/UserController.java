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
            Rol role = getCurrentUserRole();

            if (role != Rol.SUPER_ADMIN && role != Rol.ADMIN) {
                return ResponseEntity.status(403).body("🚫 No autorizado.");
            }

            var users = userService.findAll()
                    .stream()
                    .map(u -> {
                        UserDTO dto = new UserDTO();
                        dto.setId(u.getId());
                        dto.setFullName(u.getFullName());
                        dto.setEmail(u.getEmail());
                        dto.setRole(u.getRole().name());

                        if (u.getOrganization() != null) {
                            dto.setOrganizationId(u.getOrganization().getId());
                            dto.setOrganizationName(u.getOrganization().getName());
                        }

                        dto.setCourses(
                                u.getCourses() != null
                                        ? u.getCourses().stream().map(c -> c.getName()).toList()
                                        : List.of()
                        );

                        return dto;
                    })
                    .toList();

            return ResponseEntity.ok(users);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error: " + e.getMessage());
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
    public ResponseEntity<?> getByRole(@PathVariable Rol role) {
        var users = userRepository.findByRole(role)
                .stream()
                .map(u -> {
                    var dto = new UserDTO();
                    dto.setId(u.getId());
                    dto.setFullName(u.getFullName());
                    dto.setEmail(u.getEmail());
                    dto.setRole(u.getRole().name());
                    dto.setOrganizationId(u.getOrganization() != null ? u.getOrganization().getId() : null);
                    dto.setOrganizationName(u.getOrganization() != null ? u.getOrganization().getName() : null);
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(users);
    }

}
