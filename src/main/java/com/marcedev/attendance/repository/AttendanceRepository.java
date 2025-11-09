package com.marcedev.attendance.repository;

import com.marcedev.attendance.dto.CourseMonthlyAttendanceDTO;
import com.marcedev.attendance.dto.StudentMonthlyStatDTO;
import com.marcedev.attendance.entities.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de asistencias.
 * Contiene consultas personalizadas por clase, curso, alumno y organización.
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /** 🔹 Buscar asistencias por ID de clase */
    List<Attendance> findByClassSessionId(Long classId);

    /** 🔹 Buscar asistencias por ID de curso */
    List<Attendance> findByCourseId(Long courseId);

    /** 🔹 Buscar asistencias por ID de alumno */
    List<Attendance> findByStudentId(Long studentId);

    /**
     * 🔹 Buscar asistencias por ID de organización (para los ADMIN)
     * Esta query evita tener que traer todos los registros a memoria.
     */
    @Query("SELECT a FROM Attendance a WHERE a.organization.id = :orgId")
    List<Attendance> findByOrganizationId(@Param("orgId") Long orgId);
    void deleteByClassSessionId(Long classSessionId);

    @Query("""
    SELECT new com.marcedev.attendance.dto.StudentMonthlyStatDTO(
        a.student.id,
        a.student.fullName,
        SUM(CASE WHEN a.attended = true THEN 1 ELSE 0 END),
        SUM(CASE WHEN a.attended = false THEN 1 ELSE 0 END),
        COUNT(a),
        (SUM(CASE WHEN a.attended = true THEN 1 ELSE 0 END) * 100.0 / COUNT(a))
    )
    FROM Attendance a
    WHERE a.course.id = :courseId
      AND MONTH(a.classSession.date) = :month
      AND YEAR(a.classSession.date) = :year
    GROUP BY a.student.id, a.student.fullName
    ORDER BY a.student.fullName ASC
""")
    List<StudentMonthlyStatDTO> getMonthlyStats(Long courseId, int month, int year);

   ///
   @Query("""
    SELECT new com.marcedev.attendance.dto.CourseMonthlyAttendanceDTO(
        s.id,
        s.fullName,
        COALESCE(SUM(CASE WHEN a.attended = true THEN 1 ELSE 0 END), 0),
        :totalClasses,
        CASE WHEN :totalClasses = 0 THEN 0.0
             ELSE (COALESCE(SUM(CASE WHEN a.attended = true THEN 1 ELSE 0 END), 0) * 100.0 / :totalClasses)
        END
    )
    FROM User s
    LEFT JOIN Attendance a ON a.student.id = s.id AND a.course.id = :courseId
         AND MONTH(a.classSession.date) = :month
         AND YEAR(a.classSession.date) = :year
    JOIN s.courses c
    WHERE c.id = :courseId
    GROUP BY s.id, s.fullName
    ORDER BY s.fullName
""")
   List<CourseMonthlyAttendanceDTO> getMonthlyCourseStats(
           Long courseId,
           int month,
           int year,
           Long totalClasses
   );

    ///
    @Query("SELECT COUNT(DISTINCT a.classSession.id) FROM Attendance a " +
            "WHERE a.course.id = :courseId " +
            "AND MONTH(a.classSession.date) = :month " +
            "AND YEAR(a.classSession.date) = :year")
    long countClassesInMonth(Long courseId, int month, int year);

    @Query("SELECT COUNT(a) FROM Attendance a " +
            "WHERE a.student.id = :studentId " +
            "AND a.course.id = :courseId " +
            "AND a.attended = true " +
            "AND MONTH(a.classSession.date) = :month " +
            "AND YEAR(a.classSession.date) = :year")
    long countAttendances(Long studentId, Long courseId, int month, int year);


}
