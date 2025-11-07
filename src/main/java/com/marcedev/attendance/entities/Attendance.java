package com.marcedev.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attendances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Estado: true = asistió, false = ausente */
    private boolean attended;

    // 🔹 Alumno que asistió
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties({"organization", "courses", "attendances", "password"})
    private User student;

    // 🔹 Clase (sesión) a la que pertenece
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    @JsonIgnoreProperties({"course", "attendances", "organization"})
    private ClassSession classSession;

    // 🔹 Curso correcto como relación, no como ID suelto
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"organization", "users"})
    private Course course;

    // 🔹 Organización
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnoreProperties({"users", "courses", "admin"})
    private Organization organization;
}
