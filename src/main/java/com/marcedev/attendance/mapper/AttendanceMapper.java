package com.marcedev.attendance.mapper;

import com.marcedev.attendance.dto.AttendanceDTO;
import com.marcedev.attendance.entities.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    // 🔹 Entity → DTO
    @BeanMapping(ignoreByDefault = false)
    @Mapping(source = "classSession.id", target = "classId")
    @Mapping(source = "classSession.name", target = "className")
    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "course.name", target = "courseName")
    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "student.fullName", target = "studentName")
    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "organization.name", target = "organizationName", defaultValue = "Sin organización")
    AttendanceDTO toDTO(Attendance entity);

    // 🔹 DTO → Entity
    @Mapping(target = "classSession", expression = "java(mapClassSession(dto))")
    @Mapping(target = "student", expression = "java(mapStudent(dto))")
    @Mapping(target = "course", expression = "java(mapCourse(dto))") // ✅ CORREGIDO
    @Mapping(target = "organization", expression = "java(mapOrganization(dto))")
    Attendance toEntity(AttendanceDTO dto);

    // ⚙️ Métodos auxiliares

    default ClassSession mapClassSession(AttendanceDTO dto) {
        if (dto.getClassId() == null) return null;
        ClassSession session = new ClassSession();
        session.setId(dto.getClassId());
        session.setName(dto.getClassName());
        return session;
    }

    default User mapStudent(AttendanceDTO dto) {
        if (dto.getStudentId() == null) return null;
        User user = new User();
        user.setId(dto.getStudentId());
        user.setFullName(dto.getStudentName());
        return user;
    }

    default Course mapCourse(AttendanceDTO dto) {
        if (dto.getCourseId() == null) return null;
        Course course = new Course();
        course.setId(dto.getCourseId());
        course.setName(dto.getCourseName());
        return course;
    }

    default Organization mapOrganization(AttendanceDTO dto) {
        if (dto.getOrganizationId() == null) return null;
        Organization org = new Organization();
        org.setId(dto.getOrganizationId());
        org.setName(dto.getOrganizationName());
        return org;
    }
}
