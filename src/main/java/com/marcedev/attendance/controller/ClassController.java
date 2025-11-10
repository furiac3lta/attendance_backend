package com.marcedev.attendance.controller;

import com.marcedev.attendance.dto.AttendanceMarkDTO;
import com.marcedev.attendance.dto.ClassCreateDTO;
import com.marcedev.attendance.dto.ClassDetailsDTO;
import com.marcedev.attendance.entities.ClassSession;
import com.marcedev.attendance.entities.Course;
import com.marcedev.attendance.entities.User;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.repository.ClassSessionRepository;
import com.marcedev.attendance.repository.CourseRepository;
import com.marcedev.attendance.repository.UserRepository;
import com.marcedev.attendance.service.AttendanceService;
import com.marcedev.attendance.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ClassController {

    private final ClassService classService;
    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ClassSessionRepository classSessionRepository;

    // ✅ Obtener o crear la clase del día (para tomar asistencia)
    @GetMapping("/today/{courseId}")
    public ResponseEntity<?> getOrCreateTodayClass(@PathVariable Long courseId) {
        return ResponseEntity.ok(classService.getOrCreateTodaySession(courseId));
    }

    // ✅ Obtener clases por curso
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getClassesByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(classService.findByCourseId(courseId));
    }

    @GetMapping("/{id}/details")
    public ClassDetailsDTO getClassDetails(@PathVariable Long id) {
        return classService.getClassDetails(id);
    }


    @PostMapping
    public ResponseEntity<?> create(@RequestBody ClassCreateDTO dto) {
        try {
            User currentUser = getAuthenticatedUser();

            if (!hasPermission(Rol.INSTRUCTOR, Rol.ADMIN, Rol.SUPER_ADMIN)) {
                return ResponseEntity.status(403).body("🚫 No autorizado para crear clases.");
            }

            var course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

            LocalDate date = LocalDate.parse(dto.getDate());

            ClassSession newClass = new ClassSession();
            newClass.setName(dto.getName());
            newClass.setDate(date);
            newClass.setCourse(course);
            newClass.setInstructor(currentUser); // ✅ NECESARIO
            newClass.setOrganization(course.getOrganization()); // ✅ NECESARIO

            ClassSession saved = classService.create(newClass);

            return ResponseEntity.ok(Map.of(
                    "id", saved.getId(),
                    "name", saved.getName(),
                    "date", saved.getDate(),
                    "courseName", course.getName()
            ));

        } catch (Exception e) {
            e.printStackTrace(); // ✅ MOSTRAR ERROR EN CONSOLA
            return ResponseEntity.internalServerError()
                    .body("❌ Error inesperado: " + e.getMessage());
        }
    }

    // ✅ Obtener una clase por ID (para tomar asistencia)
    @GetMapping("/{id}")
    public ResponseEntity<?> getClassById(@PathVariable Long id) {
        ClassSession session = classService.findById(id);
        if (session == null) {
            return ResponseEntity.status(404).body("❌ Clase no encontrada");
        }

        // Evitar serialización recursiva
        if (session.getInstructor() != null) session.getInstructor().setPassword(null);
        if (session.getCourse() != null && session.getCourse().getInstructor() != null) {
            session.getCourse().getInstructor().setPassword(null);
        }

        return ResponseEntity.ok(session);
    }


    // ✅ Registrar asistencia (CORREGIDO)
    @PostMapping("/{classId}/attendance")
    public ResponseEntity<?> registerAttendance(
            @PathVariable Long classId,
            @RequestBody List<com.marcedev.attendance.dto.AttendanceMarkDTO> attendances
    ) {
        attendanceService.registerAttendance(classId, attendances);
        return ResponseEntity.ok().build(); // ✅ Respuesta simple, sin devolver nada
    }


    // ✅ Obtener asistencias registradas
    @GetMapping("/{classId}/attendance")
    public ResponseEntity<?> getAttendance(@PathVariable Long classId) {
        return ResponseEntity.ok(attendanceService.findByClassId(classId));
    }

    // ✅ Obtener alumnos para tomar asistencia
    @GetMapping("/{classId}/students")
    public ResponseEntity<?> getStudentsForClass(@PathVariable Long classId) {

        ClassSession classSession = classService.findById(classId);
        if (classSession == null) {
            return ResponseEntity.status(404).body("❌ Clase no encontrada");
        }

        Course course = classSession.getCourse();
        if (course == null || course.getStudents() == null) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(
                course.getStudents().stream()
                        .map(this::mapStudent)
                        .collect(Collectors.toList())
        );
    }

    // ✅ Mapear alumno → JSON simple
    private Map<String, Object> mapStudent(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("fullName", user.getFullName());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name());
        return map;
    }

    // ✅ Permisos
    private boolean hasPermission(Rol... allowed) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) return false;
        if (user.getRole() == Rol.SUPER_ADMIN) return true;

        return Arrays.asList(allowed).contains(user.getRole());
    }

    @PostMapping("/create-or-get")
    public ResponseEntity<?> createOrGetSession(@RequestBody Map<String, Long> body) {
        Long courseId = body.get("courseId");
        ClassSession session = classService.getOrCreateTodaySession(courseId);
        return ResponseEntity.ok(session);
    }
    // ✅ Obtener usuario autenticado
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("⚠️ Usuario no autenticado");
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado en BD"));
    }

}
