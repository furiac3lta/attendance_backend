package com.marcedev.attendance.service.impl;

import com.marcedev.attendance.entities.Course;
import com.marcedev.attendance.entities.Organization;
import com.marcedev.attendance.entities.User;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.repository.CourseRepository;
import com.marcedev.attendance.repository.UserRepository;
import com.marcedev.attendance.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    @Override
    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    @Override
    public Course saveCourseWithAuthenticatedInstructor(Course course) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User instructor = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Instructor no encontrado"));

        if (instructor.getOrganization() == null) {
            throw new IllegalStateException("El instructor no pertenece a ninguna organización");
        }

        course.setInstructor(instructor);
        course.setOrganization(instructor.getOrganization());

        return courseRepository.save(course);
    }

    @Override
    public Course save(Course course) {
        return courseRepository.save(course);
    }

    @Override
    public Course update(Long id, Course updatedCourse) {
        return courseRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedCourse.getName());
                    existing.setDescription(updatedCourse.getDescription());
                    existing.setUniversityProgram(updatedCourse.getUniversityProgram());
                    return courseRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado"));
    }

    @Override
    public void deleteById(Long id) {
        courseRepository.deleteById(id);
    }

    /**
     * 🔹 Inscribir un alumno al curso
     */
    @Override
    public Course addStudentToCourse(Long courseId, Long userId) {
        // 🧠 Obtener curso
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("❌ Curso no encontrado con ID: " + courseId));

        // 🧠 Obtener alumno
        User student = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("❌ Usuario no encontrado con ID: " + userId));

        // 🧱 Validar organización
        if (course.getOrganization() == null || student.getOrganization() == null) {
            throw new IllegalStateException("🚫 Curso o alumno sin organización asociada.");
        }

        if (!course.getOrganization().getId().equals(student.getOrganization().getId())) {
            throw new IllegalStateException("🚫 El alumno pertenece a otra organización.");
        }

        // 🔁 Evitar duplicados
        if (course.getStudents() != null && course.getStudents().stream()
                .anyMatch(u -> u.getId().equals(student.getId()))) {
            System.out.println("⚠️ El alumno ya estaba inscripto en el curso.");
            return course;
        }

        // 🧩 Agregar alumno al curso
        course.getStudents().add(student);

        // 💾 Guardar
        Course saved = courseRepository.save(course);
        System.out.println("✅ Alumno agregado correctamente: " + student.getEmail());

        return saved;
    }

    /**
     * 🔹 Remover un alumno del curso
     */
    @Override
    public Course removeStudentFromCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado"));

        User student = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (course.getStudents().contains(student)) {
            course.getStudents().remove(student);
            student.getCourses().remove(course);
            userRepository.save(student);
            courseRepository.save(course);
        }

        return course;
    }

    @Override
    public List<Course> findMyCourses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // ✅ SUPER_ADMIN ve todos los cursos, sin requerir organización
        if (user.getRole() == Rol.SUPER_ADMIN) {
            return courseRepository.findAll();
        }

        // ⚠️ Si no tiene organización, no puede ver cursos
        Organization org = user.getOrganization();
        if (org == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna organización");
        }

        // ✅ Si es admin → ve todos los cursos de su organización
        if (user.getRole() == Rol.ADMIN) {
            return courseRepository.findByOrganization(org);
        }

        // ✅ Si es instructor → ve sus propios cursos
        if (user.getRole() == Rol.INSTRUCTOR) {
            return courseRepository.findByOrganizationAndInstructor(org, user);
        }

        // ✅ Si es alumno → devuelve sus cursos inscritos
        if (user.getRole() == Rol.USER) {
            return user.getCourses().stream().toList();
        }

        return List.of();
    }
}
