package com.marcedev.attendance.mapper;

import com.marcedev.attendance.dto.ClassDTO;
import com.marcedev.attendance.entities.ClassSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClassMapper {

    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "course.name", target = "courseName")
    @Mapping(source = "instructor.id", target = "instructorId")
    @Mapping(source = "instructor.fullName", target = "instructorName")
    @Mapping(source = "organization.id", target = "organizationId", defaultValue = "0L")
    @Mapping(source = "organization.name", target = "organizationName", defaultValue = "Sin organización")
    ClassDTO toDto(ClassSession classSession);
}
