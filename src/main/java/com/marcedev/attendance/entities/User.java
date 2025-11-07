package com.marcedev.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.marcedev.attendance.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa un usuario del sistema.
 * Puede ser SUPER_ADMIN (global), ADMIN (de organización),
 * INSTRUCTOR (profesor) o USER (alumno).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"password", "organization", "courses", "hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre completo del usuario */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Email único del usuario */
    @Column(unique = true, nullable = false)
    private String email;

    /** Contraseña (encriptada al guardarse) */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    /** Rol del usuario (SUPER_ADMIN, ADMIN, INSTRUCTOR, USER) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol role;

    /** Cursos a los que pertenece el usuario (como alumno o instructor) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_courses",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @JsonIgnoreProperties({
            "students", "instructor", "organization"
    })
    private Set<Course> courses = new HashSet<>();

    /** Asistencias del alumno (solo aplica para usuarios con rol USER) */
    @OneToMany(mappedBy = "student", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Attendance> attendances;

    /** Organización a la que pertenece el usuario */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @JsonIgnoreProperties({
            "users", "courses", "admin"
    })
    private Organization organization;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                '}';
    }
}
