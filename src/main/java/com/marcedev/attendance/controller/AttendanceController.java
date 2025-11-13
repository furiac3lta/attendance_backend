package com.marcedev.attendance.controller;

import com.marcedev.attendance.dto.AttendanceDTO;
import com.marcedev.attendance.dto.AttendanceMarkDTO;
import com.marcedev.attendance.entities.User;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.service.impl.AttendanceServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor

public class AttendanceController {

    private final AttendanceServiceImpl attendanceService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody AttendanceDTO dto) {
        if (!hasPermission(Rol.INSTRUCTOR, Rol.ADMIN, Rol.SUPER_ADMIN)) {
            return ResponseEntity.status(403).body("🚫 No tiene permisos para crear asistencias.");
        }
        AttendanceDTO saved = attendanceService.save(dto);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<AttendanceDTO>> getAll() {
        if (!hasPermission(Rol.INSTRUCTOR, Rol.ADMIN, Rol.SUPER_ADMIN)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(attendanceService.findAll());
    }

    @GetMapping("/class/{id}")
    public ResponseEntity<List<AttendanceDTO>> getByClass(@PathVariable Long id) {
        if (!hasPermission(Rol.INSTRUCTOR, Rol.ADMIN, Rol.SUPER_ADMIN)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(attendanceService.findByClassId(id));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<AttendanceDTO>> getByCourse(@PathVariable Long courseId) {
        if (!hasPermission(Rol.INSTRUCTOR, Rol.ADMIN, Rol.SUPER_ADMIN)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(attendanceService.findByCourseId(courseId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        if (!hasPermission(Rol.INSTRUCTOR, Rol.ADMIN, Rol.SUPER_ADMIN)) {
            return ResponseEntity.status(403).body("🚫 No tiene permisos para ver asistencias individuales.");
        }
        try {
            return ResponseEntity.ok(attendanceService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("❌ Asistencia no encontrada.");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!hasPermission(Rol.ADMIN, Rol.SUPER_ADMIN)) {
            return ResponseEntity.status(403).body("🚫 No tiene permisos para eliminar asistencias.");
        }
        attendanceService.deleteById(id);
        return ResponseEntity.ok("✅ Asistencia eliminada correctamente.");
    }

    private boolean hasPermission(Rol... allowedRoles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        Object principal = auth.getPrincipal();
        if (!(principal instanceof org.springframework.security.core.userdetails.User userDetails)) return false;

        String email = userDetails.getUsername();
        User currentUser = attendanceService.getAuthenticatedUserFromEmail(email).orElse(null);
        if (currentUser == null) return false;

        if (currentUser.getRole() == Rol.SUPER_ADMIN) return true; // acceso total

        for (Rol role : allowedRoles) {
            if (currentUser.getRole() == role) return true;
        }
        return false;
    }

    // ✅ Obtener o crear la sesión del día (devuelve sólo lo necesario para evitar problemas de serialización)
    @PostMapping("/{classId}/sessions")
    public ResponseEntity<Map<String, Object>> createOrGetTodaySession(@PathVariable Long classId) {
        var session = attendanceService.getOrCreateTodaySession(classId);
        return ResponseEntity.ok(Map.of(
                "id", session.getId(),
                "date", session.getDate()
        ));
    }

    // ✅ Registrar asistencia para la sesión (usa DTO mínimo que coincide con el front)
    @PostMapping("/{sessionId}/attendance")
    public ResponseEntity<?> registerAttendance(
            @PathVariable Long sessionId,
            @RequestBody List<AttendanceMarkDTO> attendances
    ) {
        attendanceService.registerAttendance(sessionId, attendances);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/course/{courseId}/monthly")
    public ResponseEntity<?> getMonthlyStats(
            @PathVariable Long courseId,
            @RequestParam int month,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(attendanceService.getCourseMonthlyStats(courseId, month, year));
    }

    @GetMapping("/course/{courseId}/report")
    public ResponseEntity<?> getCourseReport(
            @PathVariable Long courseId,
            @RequestParam int month,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(attendanceService.getCourseMonthlyStats(courseId, month, year));
    }

}
