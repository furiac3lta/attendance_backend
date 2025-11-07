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

/**
 * Controlador REST para la gestión de usuarios.
 * Incluye validaciones por rol y manejo de organizaciones.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository; // ✅ ahora sí inyectado correctamente
    private final CourseRepository courseRepository;


    // 🔹 Obtener todos los usuarios (solo SUPER_ADMIN y ADMIN)
    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            Rol currentRole = getCurrentUserRole();

            if (currentRole != Rol.SUPER_ADMIN && currentRole != Rol.ADMIN) {
                return ResponseEntity.status(403)
                        .body("🚫 No tiene permisos para listar usuarios.");
            }

            List<User> users = userService.findAll();
            users.forEach(u -> u.getCourses().forEach(c -> c.setInstructor(null)));

            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error al obtener usuarios: " + e.getMessage());
        }
    }

    // 🔹 Obtener usuario por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            var optionalUser = userService.findById(id);

            if (optionalUser.isEmpty()) {
                return ResponseEntity
                        .status(404)
                        .body("❌ Usuario no encontrado con ID " + id);
            }

            var user = optionalUser.get();
            user.getCourses().forEach(c -> c.setInstructor(null));

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body("❌ Error: " + e.getMessage());
        }
    }

    // 🔹 Crear usuario (controlado por rol)
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody User user) {
        try {
            Rol currentRole = getCurrentUserRole();

            if (currentRole == Rol.INSTRUCTOR || currentRole == Rol.USER) {
                return ResponseEntity.status(403)
                        .body("🚫 No tiene permisos para crear usuarios.");
            }

            if (currentRole == Rol.ADMIN) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String email = auth.getName();
                User currentUser = userService.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

                if (user.getOrganization() == null) {
                    user.setOrganization(currentUser.getOrganization());
                }

                if (user.getRole() == Rol.SUPER_ADMIN) {
                    return ResponseEntity.status(403)
                            .body("🚫 No puede crear usuarios con rol SUPER_ADMIN.");
                }
            }

            User saved = userService.save(user);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error al crear usuario: " + e.getMessage());
        }
    }

    @GetMapping("/role/{role}")
    public List<User> getByRole(@PathVariable Rol role) {
        return userRepository.findByRole(role);
    }

    // 🔹 Nuevo: Crear administrador dentro de una organización
    @PostMapping("/organizations/{orgId}/admins")
    public ResponseEntity<?> createAdminForOrganization(
            @PathVariable Long orgId,
            @RequestBody User adminData
    ) {
        try {
            Rol currentRole = getCurrentUserRole();

            // Solo SUPER_ADMIN o ADMIN pueden usar este endpoint
            if (currentRole != Rol.SUPER_ADMIN && currentRole != Rol.ADMIN) {
                return ResponseEntity.status(403)
                        .body("🚫 No tiene permisos para crear administradores.");
            }

            User newAdmin = userService.createAdminForOrganization(orgId, adminData);
            return ResponseEntity.ok(newAdmin);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("❌ Error al crear administrador: " + e.getMessage());
        }
    }

    // 🔹 Eliminar usuario (solo SUPER_ADMIN y ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            Rol currentRole = getCurrentUserRole();
            if (currentRole != Rol.SUPER_ADMIN && currentRole != Rol.ADMIN) {
                return ResponseEntity.status(403)
                        .body("🚫 No tiene permisos para eliminar usuarios.");
            }

            userService.deleteById(id);
            return ResponseEntity.ok("✅ Usuario eliminado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error al eliminar usuario: " + e.getMessage());
        }
    }

    // 🔹 Asignar cursos (solo ADMIN o INSTRUCTOR)
    @PostMapping("/{userId}/assign-courses")
    public ResponseEntity<?> assignCoursesToUser(
            @PathVariable Long userId,
            @RequestBody List<Long> courseIds) {
        try {
            Rol currentRole = getCurrentUserRole();

            if (currentRole != Rol.ADMIN && currentRole != Rol.INSTRUCTOR) {
                return ResponseEntity.status(403)
                        .body("🚫 Solo ADMIN o INSTRUCTOR pueden asignar cursos.");
            }

            User updatedUser = userService.assignCourses(userId, courseIds);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error al asignar cursos: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody User updatedUser
    ) {
        try {
            userService.updateUser(id, updatedUser);
            return ResponseEntity.ok("✅ Usuario actualizado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("❌ Error al actualizar usuario: " + e.getMessage());
        }
    }

    // 🧩 Método auxiliar para obtener el rol del usuario autenticado
    private Rol getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No hay usuario autenticado");
        }

        String email = auth.getName();
        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));

        return currentUser.getRole();
    }
    // 🔹 Listar todos los usuarios de una organización (solo SUPER_ADMIN o ADMIN)
    @GetMapping("/organization/{orgId}")
    public ResponseEntity<?> getByOrganization(@PathVariable Long orgId) {
        try {
            Rol currentRole = getCurrentUserRole();

            if (currentRole != Rol.SUPER_ADMIN && currentRole != Rol.ADMIN) {
                return ResponseEntity.status(403)
                        .body("🚫 No tiene permisos para listar usuarios por organización.");
            }

            List<User> users = userService.findAll()
                    .stream()
                    .filter(u -> u.getOrganization() != null && orgId.equals(u.getOrganization().getId()))
                    .toList();

            // Evitamos referencias circulares en JSON
            users.forEach(u -> u.getCourses().forEach(c -> c.setInstructor(null)));

            return ResponseEntity.ok(users);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("❌ Error al obtener usuarios por organización: " + e.getMessage());
        }
    }


    // 🔹 Promover un usuario existente a ADMIN
    @PutMapping("/{id}/promote-to-admin")
    public ResponseEntity<?> promoteToAdmin(@PathVariable Long id) {
        try {
            Rol currentRole = getCurrentUserRole();

            // Solo SUPER_ADMIN puede promover a ADMIN
            if (currentRole != Rol.SUPER_ADMIN) {
                return ResponseEntity.status(403)
                        .body("🚫 Solo SUPER_ADMIN puede promover usuarios a ADMIN.");
            }

            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

            // Si ya es admin o super admin, no tiene sentido promover
            if (user.getRole() == Rol.ADMIN || user.getRole() == Rol.SUPER_ADMIN) {
                return ResponseEntity.badRequest()
                        .body("⚠️ Este usuario ya es administrador.");
            }

            user.setRole(Rol.ADMIN);
            userService.updateUser(id, user);

            return ResponseEntity.ok("✅ Usuario promovido a ADMIN correctamente.");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("❌ Error al promover usuario: " + e.getMessage());
        }
    }
    // 🔹 Degradar (rebajar) un usuario a un rol inferior (INSTRUCTOR o USER)
    @PutMapping("/{id}/demote")
    public ResponseEntity<?> demoteUser(
            @PathVariable Long id,
            @RequestParam Rol newRole
    ) {
        try {
            Rol currentRole = getCurrentUserRole();

            // Solo SUPER_ADMIN puede degradar roles
            if (currentRole != Rol.SUPER_ADMIN) {
                return ResponseEntity.status(403)
                        .body("🚫 Solo SUPER_ADMIN puede degradar usuarios.");
            }

            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

            // Validaciones básicas
            if (user.getRole() == Rol.SUPER_ADMIN) {
                return ResponseEntity.badRequest()
                        .body("🚫 No se puede degradar a un SUPER_ADMIN.");
            }

            if (newRole == Rol.SUPER_ADMIN) {
                return ResponseEntity.badRequest()
                        .body("🚫 No puede asignar el rol SUPER_ADMIN desde este endpoint.");
            }

            // Aplicar el nuevo rol
            user.setRole(newRole);
            userService.updateUser(id, user);

            return ResponseEntity.ok("✅ Rol actualizado a " + newRole + " correctamente.");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("❌ Error al degradar usuario: " + e.getMessage());
        }
    }

    @GetMapping("/visible")
    public ResponseEntity<?> getVisibleUsers() {
        var users = userRepository.findAll();

        var result = users.stream().map(u -> {
            var dto = new UserDTO();
            dto.setId(u.getId());
            dto.setFullName(u.getFullName());
            dto.setEmail(u.getEmail());
            dto.setRole(u.getRole().name());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/members")
    public ResponseEntity<?> getVisibleMembers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("🚫 No autenticado");
        }

        String email = auth.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<User> members;

        switch (currentUser.getRole()) {
            case SUPER_ADMIN -> {
                // ✅ SuperAdmin ve todos
                members = userRepository.findAll();
            }
            case ADMIN -> {
                // ✅ Admin ve solo su organización
                if (currentUser.getOrganization() == null) {
                    return ResponseEntity.badRequest()
                            .body("⚠️ Este administrador no tiene organización asignada.");
                }
                members = userRepository.findByOrganizationId(currentUser.getOrganization().getId());
            }
            default -> {
                // 🚫 Instructores y usuarios comunes no pueden acceder
                return ResponseEntity.status(403)
                        .body("🚫 No tiene permisos para ver los miembros.");
            }
        }

        return ResponseEntity.ok(members);
    }

    // 🧩 Helper para obtener el usuario autenticado actual
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }
    // 🔹 Obtener alumnos por curso (para tomar asistencia)
    // 🔹 Obtener alumnos por curso (para tomar asistencia)
    @GetMapping("/by-course/{courseId}")
    public ResponseEntity<?> getUsersByCourse(@PathVariable Long courseId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body("🚫 Usuario no autenticado");
            }

            String email = auth.getName();
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));

            Rol currentRole = currentUser.getRole();

            // Solo ADMIN, INSTRUCTOR o SUPER_ADMIN pueden ver los alumnos de un curso
            if (currentRole != Rol.ADMIN && currentRole != Rol.INSTRUCTOR && currentRole != Rol.SUPER_ADMIN) {
                return ResponseEntity.status(403)
                        .body("🚫 No tiene permisos para ver los alumnos de este curso.");
            }

            // Buscar usuarios asociados al curso
            List<User> users = userRepository.findDistinctByCoursesIdIn(List.of(courseId));

            // Evitar recursión infinita en JSON
            users.forEach(u -> {
                if (u.getCourses() != null) {
                    u.getCourses().forEach(c -> c.setInstructor(null));
                }
            });

            return ResponseEntity.ok(users);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("❌ Error al obtener alumnos del curso: " + e.getMessage());
        }
    }

}
