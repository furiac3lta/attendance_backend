package com.marcedev.attendance.service;

import com.marcedev.attendance.entities.Course;
import com.marcedev.attendance.entities.Organization;
import com.marcedev.attendance.entities.User;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.repository.CourseRepository;
import com.marcedev.attendance.repository.OrganizationRepository;
import com.marcedev.attendance.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    /**
     * 🔹 Elimina una organización y limpia todas sus relaciones.
     */
    @Transactional
    public void deleteById(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organización no encontrada"));

        // 🧩 1️⃣ Desvincular cursos
        List<Course> courses = courseRepository.findByOrganizationId(id);
        for (Course c : courses) {
            c.setInstructor(null);
            c.setOrganization(null);
        }
        courseRepository.saveAll(courses);

        // 🧩 2️⃣ Desvincular usuarios
        List<User> users = userRepository.findByOrganizationId(id);
        for (User u : users) {
            u.setOrganization(null);
        }
        userRepository.saveAll(users);

        // 🧩 3️⃣ Finalmente eliminar la organización
        organizationRepository.delete(org);
    }

    /**
     * 🔹 Asigna un administrador (rol ADMIN) a una organización
     */
    @Transactional
    public void assignAdmin(Long organizationId, Long userId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organización no encontrada."));
        User admin = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (admin.getRole() != Rol.ADMIN) {
            throw new RuntimeException("El usuario seleccionado no tiene rol ADMIN.");
        }

        // ✅ permitir cambio (si querés bloquear, poné una validación acá)
        org.setAdmin(admin);
        admin.setOrganization(org);

        organizationRepository.saveAndFlush(org);   // fuerza UPDATE organizations (admin_id)
        userRepository.saveAndFlush(admin);         // fuerza UPDATE users (organization_id)
    }

}
