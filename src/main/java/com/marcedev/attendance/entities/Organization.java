package com.marcedev.attendance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String address;
    private String phone;
    private String logoUrl;

    /** Admin principal de la organización */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "classes"})
    private User admin;

    /** Usuarios de esta organización */
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonIgnoreProperties({
            "organization", "courses", "attendances"
    })
    private List<User> users;

    /** Cursos de esta organización */
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonIgnoreProperties({
            "organization", "instructor", "students"
    })
    private List<Course> courses;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "Organization{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
