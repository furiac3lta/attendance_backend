package com.marcedev.attendance.service.impl;

import com.marcedev.attendance.dto.AttendanceDTO;
import com.marcedev.attendance.dto.AttendanceMarkDTO;
import com.marcedev.attendance.dto.StudentMonthlyStatDTO;
import com.marcedev.attendance.entities.*;
import com.marcedev.attendance.enums.Rol;
import com.marcedev.attendance.mapper.AttendanceMapper;
import com.marcedev.attendance.repository.AttendanceRepository;
import com.marcedev.attendance.repository.ClassSessionRepository;
import com.marcedev.attendance.repository.CourseRepository;
import com.marcedev.attendance.repository.UserRepository;
import com.marcedev.attendance.service.AttendanceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final UserRepository userRepository;
    private final AttendanceMapper attendanceMapper;
    private final CourseRepository courseRepository;

    // ================== CRUD ==================

    @Override
    public AttendanceDTO save(AttendanceDTO dto) {
        Attendance entity = attendanceMapper.toEntity(dto);
        Attendance saved = attendanceRepository.save(entity);
        return attendanceMapper.toDTO(saved);
    }

    @Override
    public List<AttendanceDTO> findAll() {
        User currentUser = getAuthenticatedUser();

        List<Attendance> attendances = switch (currentUser.getRole()) {
            case SUPER_ADMIN -> attendanceRepository.findAll();
            case ADMIN -> attendanceRepository.findByOrganizationId(currentUser.getOrganization().getId());
            case INSTRUCTOR -> {
                Long orgId = currentUser.getOrganization() != null ? currentUser.getOrganization().getId() : null;
                yield (orgId != null) ? attendanceRepository.findByOrganizationId(orgId) : List.of();
            }
            default -> throw new RuntimeException("No tiene permisos para ver asistencias");
        };

        return attendances.stream()
                .map(attendanceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDTO> findByClassId(Long classId) {
        return attendanceRepository.findByClassSessionId(classId)
                .stream().map(attendanceMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDTO> findByCourseId(Long courseId) {
        return attendanceRepository.findByCourseId(courseId)
                .stream().map(attendanceMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public AttendanceDTO findById(Long id) {
        return attendanceRepository.findById(id)
                .map(attendanceMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Asistencia no encontrada"));
    }

    @Override
    public void deleteById(Long id) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() == Rol.USER) {
            throw new RuntimeException("No autorizado");
        }
        attendanceRepository.deleteById(id);
    }

    // ================== AUTH ==================

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No hay usuario autenticado");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Override
    public Optional<User> getAuthenticatedUserFromEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ================== REGISTRO DE ASISTENCIA ==================

    /** COMPATIBILIDAD vieja API */
    @Override
    public void registerAttendanceByCourse(Long courseId, Map<Long, Boolean> attendanceMap) {

        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        LocalDate today = LocalDate.now();

        // ✅ Obtener o crear la sesión de hoy
        ClassSession session = classSessionRepository.findByCourseIdAndDate(courseId, today)
                .orElseGet(() -> {
                    var newSession = new ClassSession();
                    newSession.setCourse(course);
                    newSession.setDate(today);
                    newSession.setName("Clase automática " + course.getName());
                    newSession.setInstructor(
                            course.getInstructor() != null
                                    ? course.getInstructor()
                                    : getAuthenticatedUser()
                    );
                    newSession.setOrganization(course.getOrganization());
                    return classSessionRepository.save(newSession);
                });

        // ✅ Organización segura
        Organization org = session.getOrganization() != null
                ? session.getOrganization()
                : course.getOrganization();

        // ✅ Registrar asistencias
        attendanceMap.forEach((studentId, present) -> {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

            Attendance att = new Attendance();
            att.setClassSession(session);   // ✅ AHORA sí existe
            att.setStudent(student);
            att.setAttended(present);
            att.setCourse(course);          // ✅ Correcto
            att.setOrganization(org);       // ✅ Correcto

            attendanceRepository.save(att);
        });
    }

    /** ✅ API NUEVA (la que usa tu frontend) */

    @Override
    @Transactional
    public void registerAttendance(Long sessionId, List<AttendanceMarkDTO> attendances) {

        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        Course course = session.getCourse();
        if (course == null) {
            throw new RuntimeException("La sesión no tiene curso asociado.");
        }

        Organization org = session.getOrganization();
        if (org == null) {
            org = course.getOrganization(); // fallback por seguridad
        }

        for (AttendanceMarkDTO mark : attendances) {

            User student = userRepository.findById(mark.getUserId())
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado ID: " + mark.getUserId()));

            Attendance attendance = new Attendance();
            attendance.setClassSession(session);   // FK ✅
            attendance.setStudent(student);        // FK ✅
            attendance.setAttended(mark.isPresent());
            attendance.setCourse(course);          // ✅ ESTA ES LA CLAVE
            attendance.setOrganization(org);       // ✅ CORRECTO

            attendanceRepository.save(attendance);
        }
    }

    // ================== SESIÓN DEL DÍA ==================

    @Override
    public ClassSession getOrCreateTodaySession(Long courseId) {
        var today = LocalDate.now();

        return classSessionRepository.findByCourseIdAndDate(courseId, today)
                .orElseGet(() -> {
                    var course = courseRepository.findById(courseId)
                            .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

                    var instructor = (course.getInstructor() != null)
                            ? course.getInstructor()
                            : getAuthenticatedUser();

                    var newSession = ClassSession.builder()
                            .course(course)
                            .date(today)
                            .instructor(instructor)
                            .organization(course.getOrganization())
                            .name(course.getName() + " – " + today)
                            .build();

                    return classSessionRepository.save(newSession);
                });
    }

    @Override
    public List<StudentMonthlyStatDTO> getCourseMonthlyStats(Long courseId, int month, int year) {
        return attendanceRepository.getMonthlyStats(courseId, month, year);
    }

}
