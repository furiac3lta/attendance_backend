package com.marcedev.attendance.repository;

import com.marcedev.attendance.entities.ClassSession;
import com.marcedev.attendance.entities.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ClassSession, Long> {

    // 👇 Esto asume que tu entidad ClassSession tiene un atributo "course"
    @Query("SELECT c.course FROM ClassSession c WHERE c.course.id = :courseId")
    Optional<Course> findCourseById(Long courseId);
}
