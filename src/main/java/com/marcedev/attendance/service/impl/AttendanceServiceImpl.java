package com.marcedev.attendance.service.impl;

import com.marcedev.attendance.dto.AttendanceDTO;
import com.marcedev.attendance.dto.AttendanceMarkDTO;
import com.marcedev.attendance.dto.CourseMonthlyAttendanceDTO;
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
    @Transactional
    public AttendanceDTO save(AttendanceDTO dto) {

        if (dto.getClassSessionId() == null || dto.getStudentId() == null) {
            throw new RuntimeException("classSessionId y studentId son obligatorios");
        }

        // Buscar si ya existe asistencia registrada para esta clase y alumno
        var existingOpt = attendanceRepository.findByStudentIdAndClassSessionId(
                dto.getStudentId(),
                dto.getClassSessionId()
        );

        Attendance entity;

        if (existingOpt.isPresent()) {
            // ✅ Ya existe → actualizar estado
            entity = existingOpt.get();
            entity.setAttended(dto.isAttended());
        } else {
            // ✅ No existe → crear nueva
            entity = attendanceMapper.toEntity(dto);

            // Aseguramos referencias clave
            var session = classSessionRepository.findById(dto.getClassSessionId())
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            var student = userRepository.findById(dto.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

            entity.setClassSession(session);
            entity.setStudent(student);
            entity.setCourse(session.getCourse());
            entity.setOrganization(session.getOrganization());
        }

        var saved = attendanceRepository.save(entity);
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
        Organization org = session.getOrganization() != null ? session.getOrganization() : course.getOrganization();

        // ✅ Traemos asistencias ya creadas (si existen)
        List<Attendance> existing = attendanceRepository.findByClassSessionId(sessionId);

        if (!existing.isEmpty()) {
            // ✅ EDITAR (UPDATE)
            for (AttendanceMarkDTO mark : attendances) {
                existing.stream()
                        .filter(a -> a.getStudent().getId().equals(mark.getUserId()))
                        .findFirst()
                        .ifPresent(a -> a.setAttended(mark.isPresent())); // solo actualiza presente/ausente
            }

            attendanceRepository.saveAll(existing);
            return;
        }

        // ✅ CREAR si no existían
        for (AttendanceMarkDTO mark : attendances) {
            User student = userRepository.findById(mark.getUserId())
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

            Attendance a = new Attendance();
            a.setClassSession(session);
            a.setStudent(student);
            a.setAttended(mark.isPresent());
            a.setCourse(course);
            a.setOrganization(org);
            attendanceRepository.save(a);
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
    public List<CourseMonthlyAttendanceDTO> getCourseMonthlyStats(Long courseId, int month, int year) {
        Long totalClasses = attendanceRepository.countClassesInMonth(courseId, month, year);
        return attendanceRepository.getMonthlyCourseStats(courseId, month, year);
    }

}
