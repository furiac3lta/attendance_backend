package com.marcedev.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"students", "organization", "hibernateLazyInitializer", "handler"})
public class Course {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "course")
    private List<ClassSession> classes;

    /** Nombre del curso (Ej: "Jiu-Jitsu Adultos", "Matemática 2º año") */
    @Column(nullable = false)
    private String name;

    /** Descripción general del curso */
    private String description;

    /** Programa o categoría (ej: “BJJ”, “Ciencias”, “Programación”) */
    @Column(name = "university_program", length = 50)
    private String universityProgram;

    /** Instructor que creó el curso */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "instructor_id", nullable = false)
    @JsonIgnoreProperties({
            "courses", "organization", "password", "attendances"
    })
    private User instructor;

    /** Alumnos inscriptos */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_courses",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({
            "courses", "organization", "password", "attendances"
    })
    private List<User> students;

    /** Organización a la que pertenece el curso */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnoreProperties({
            "users", "courses", "admin"
    })
    private Organization organization;

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", instructor=" + (instructor != null ? instructor.getFullName() : "null") +
                '}';
    }
}
