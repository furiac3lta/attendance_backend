package com.marcedev.attendance.mapper;

import com.marcedev.attendance.dto.CourseDTO;
import com.marcedev.attendance.entities.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mappings({
            @Mapping(target = "instructorId", source = "instructor.id"),
            @Mapping(target = "instructorName", source = "instructor.fullName"),
            @Mapping(target = "organizationId", source = "organization.id", defaultValue = "0L"),
            @Mapping(target = "organizationName", source = "organization.name", defaultValue = "Sin organización")
    })
    CourseDTO toDTO(Course course);

    @Mappings({
            @Mapping(target = "instructor.id", source = "instructorId"),
            @Mapping(target = "organization.id", source = "organizationId"),
            @Mapping(target = "organization.name", source = "organizationName")
    })
    Course toEntity(CourseDTO dto);
}
