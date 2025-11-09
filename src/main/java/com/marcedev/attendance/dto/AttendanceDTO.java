package com.marcedev.attendance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDTO {

    private Long id;

    // 🔹 Información de la clase
    private Long classId;
    private String className;

    // 🔹 Información del alumno
    private Long studentId;
    private String studentName;

    // 🔹 Estado de asistencia
    private boolean attended;

    // 🔹 Información del curso (mantiene compatibilidad)
    private Long courseId;
    private String courseName;

    // 🔹 NUEVO: Información de la organización (gimnasio/colegio)
    private Long organizationId;
    private String organizationName;

    private Long classSessionId; // <--- ESTE ES EL QUE FALTABA

}
