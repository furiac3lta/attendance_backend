package com.marcedev.attendance.repository;

import com.marcedev.attendance.entities.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    // ✅ Obtener las clases de un curso
    List<ClassSession> findByCourseId(Long courseId);

    // ✅ Obtener alumnos inscritos a la clase (por curso asociado)
    @Query(value = """
        SELECT u.id, u.full_name
        FROM users u
        JOIN user_courses uc ON uc.user_id = u.id
        JOIN classes cs ON cs.course_id = uc.course_id
        WHERE cs.id = :classId
        ORDER BY u.full_name
    """, nativeQuery = true)
    List<Object[]> findStudentsByClassId(@Param("classId") Long classId);

    @Query("SELECT c FROM ClassSession c WHERE c.course.id = :courseId AND c.date = :date")
    Optional<ClassSession> findByCourseIdAndDate(Long courseId, LocalDate date);

    

}
