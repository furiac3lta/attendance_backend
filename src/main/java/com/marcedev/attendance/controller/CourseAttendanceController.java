package com.marcedev.attendance.controller;

import com.marcedev.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseAttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/{courseId}/attendance")
    public ResponseEntity<?> registerAttendanceByCourse(
            @PathVariable Long courseId,
            @RequestBody Map<Long, Boolean> attendanceMap) {

        try {
            // 🟢 crea la clase si no existe una para hoy
            attendanceService.registerAttendanceByCourse(courseId, attendanceMap);
            return ResponseEntity.ok("✅ Asistencia registrada correctamente para el curso ID " + courseId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error al registrar asistencia: " + e.getMessage());
        }
    }
}
