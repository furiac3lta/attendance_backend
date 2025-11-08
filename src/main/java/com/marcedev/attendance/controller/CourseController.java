package com.marcedev.attendance.controller;

import com.marcedev.attendance.entities.Course;
import com.marcedev.attendance.entities.User;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.mapper.CourseMapper;
import com.marcedev.attendance.repository.CourseRepository;
import com.marcedev.attendance.repository.UserRepository;
import com.marcedev.attendance.service.CourseService;
import com.marcedev.attendance.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CourseController {

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;

    // ✅ Listar cursos según rol
    @GetMapping
    public ResponseEntity<?> getAll() {
        User currentUser = getAuthenticatedUser();

        return switch (currentUser.getRole()) {
            case SUPER_ADMIN -> ResponseEntity.ok(
                    courseMapper.toDTOList(courseRepository.findAll())
            );

            case ADMIN -> {
                if (currentUser.getOrganization() == null)
                    yield ResponseEntity.badRequest().body("⚠️ No tiene organización asignada.");

                yield ResponseEntity.ok(
                        courseMapper.toDTOList(
                                courseRepository.findByOrganizationId(currentUser.getOrganization().getId())
                        )
                );
            }

            case INSTRUCTOR -> ResponseEntity.ok(
                    courseMapper.toDTOList(
                            courseRepository.findByInstructorId(currentUser.getId())
                    )
            );

            default -> ResponseEntity.status(403).body("🚫 No tiene permisos para ver cursos.");
        };
    }

    // ✅ Obtener curso por ID (manejo de Optional)
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        var courseOpt = courseService.findById(id);

        if (courseOpt.isEmpty())
            return ResponseEntity.status(404).body("❌ Curso no encontrado");

        var course = courseOpt.get();

        // Forzamos Lazy Load
        if (course.getOrganization() != null) course.getOrganization().getName();
        if (course.getStudents() != null) course.getStudents().size();

        return ResponseEntity.ok(course);
    }

    // ✅ Crear curso
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Course course) {
        User currentUser = getAuthenticatedUser();

        // ✅ SUPER_ADMIN crea cursos SIN restricciones
        if (currentUser.getRole() == Rol.SUPER_ADMIN) {

            // Si no trae organización en el body, usamos la del usuario
            if (course.getOrganization() == null) {
                if (currentUser.getOrganization() == null) {
                    return ResponseEntity.badRequest()
                            .body("⚠️ El SUPER_ADMIN no tiene organización asignada.");
                }
                course.setOrganization(currentUser.getOrganization());
            }

            // Instructor SIEMPRE será el super admin
            course.setInstructor(currentUser);

        } else {
            // ✅ ADMIN o INSTRUCTOR
            if (currentUser.getOrganization() == null) {
                return ResponseEntity.badRequest()
                        .body("⚠️ Tu usuario no tiene organización asociada.");
            }

            // Asignar organización e instructor por defecto
            course.setOrganization(currentUser.getOrganization());
            course.setInstructor(currentUser);
        }

        Course saved = courseService.save(course);
        return ResponseEntity.ok(saved);
    }

    // ✅ Actualizar curso
    @PutMapping("/{id}")
    public ResponseEntity<Course> update(@PathVariable Long id, @RequestBody Course course) {
        return ResponseEntity.ok(courseService.update(id, course));
    }

    // ✅ Obtener alumnos del curso
    @GetMapping("/{courseId}/students")
    public ResponseEntity<?> getStudentsByCourse(@PathVariable Long courseId) {
        var courseOpt = courseService.findById(courseId);

        if (courseOpt.isEmpty())
            return ResponseEntity.status(404).body("❌ Curso no encontrado");

        var students = courseOpt.get().getStudents();

        if (students == null || students.isEmpty())
            return ResponseEntity.ok(List.of());

        var result = students.stream().map(s -> new HashMap<String, Object>() {{
            put("id", s.getId());
            put("fullName", s.getFullName());
            put("email", s.getEmail());
            put("role", s.getRole().name());
        }}).toList();

        return ResponseEntity.ok(result);
    }

    // ✅ Eliminar curso
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        User currentUser = getAuthenticatedUser();

        if (currentUser.getRole() == Rol.USER)
            return ResponseEntity.status(403).body("🚫 No tiene permisos.");

        courseService.deleteById(id);
        return ResponseEntity.ok("✅ Curso eliminado.");
    }

    // ✅ Inscribir alumno
    @PutMapping("/{courseId}/students/{userId}")
    public ResponseEntity<Course> addStudent(@PathVariable Long courseId, @PathVariable Long userId) {
        return ResponseEntity.ok(courseService.addStudentToCourse(courseId, userId));
    }

    // ✅ Quitar alumno
    @DeleteMapping("/{courseId}/students/{userId}")
    public ResponseEntity<Course> removeStudent(@PathVariable Long courseId, @PathVariable Long userId) {
        return ResponseEntity.ok(courseService.removeStudentFromCourse(courseId, userId));
    }

    // ✅ Cursos del instructor/admin autenticado
    @GetMapping("/mine")
    public ResponseEntity<?> getMyCourses() {
        User currentUser = getAuthenticatedUser();

        // Solo ADMIN o INSTRUCTOR pueden ver sus cursos
        if (currentUser.getRole() != Rol.ADMIN && currentUser.getRole() != Rol.INSTRUCTOR)
            return ResponseEntity.status(403).body("🚫 Solo ADMIN o INSTRUCTOR.");

        // Filtrar cursos donde el instructor coincida con el usuario autenticado
        var filteredCourses = courseService.findAll().stream()
                .filter(course -> course.getInstructor() != null &&
                        course.getInstructor().getId().equals(currentUser.getId()))
                .toList();

        // ✅ Convertir a DTO (esto hace que instructorName/instructorId lleguen completos al Frontend)
        return ResponseEntity.ok(courseMapper.toDTOList(filteredCourses));
    }

    // ✅ Obtener usuario autenticado
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new RuntimeException("Usuario no autenticado");

        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @PatchMapping("/{courseId}/assign-instructor/{instructorId}")
    public ResponseEntity<?> assignInstructor(
            @PathVariable Long courseId,
            @PathVariable Long instructorId
    ) {
        courseService.assignInstructor(courseId, instructorId);
        return ResponseEntity.ok(Map.of("message", "Instructor asignado"));
    }

}
