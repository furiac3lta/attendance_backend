package com.marcedev.attendance.repository;

import com.marcedev.attendance.entities.Course;
import com.marcedev.attendance.entities.Organization;
import com.marcedev.attendance.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByInstructorId(Long instructorId);

    // 🔹 Buscar todos los cursos de una organización
    List<Course> findByOrganization(Organization organization);

    // 🔹 Buscar todos los cursos por instructor dentro de una organización
    List<Course> findByOrganizationAndInstructor(Organization organization, User instructor);

    List<Course> findByOrganizationId(Long organizationId);

}
