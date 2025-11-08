package com.marcedev.attendance.mapper;

import com.marcedev.attendance.dto.CourseDTO;
import com.marcedev.attendance.entities.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "instructorId", expression = "java(course.getInstructor() != null ? course.getInstructor().getId() : null)")
    @Mapping(target = "instructorName", expression = "java(course.getInstructor() != null ? course.getInstructor().getFullName() : \"Sin asignar\")")
    @Mapping(target = "organizationId", expression = "java(course.getOrganization() != null ? course.getOrganization().getId() : null)")
    @Mapping(target = "organizationName", expression = "java(course.getOrganization() != null ? course.getOrganization().getName() : \"Sin organización\")")
    CourseDTO toDTO(Course course);

    List<CourseDTO> toDTOList(List<Course> courses);
    @Mappings({
            @Mapping(target = "instructor.id", source = "instructorId"),
            @Mapping(target = "organization.id", source = "organizationId"),
            @Mapping(target = "organization.name", source = "organizationName")
    })
    Course toEntity(CourseDTO dto);
}
