package com.marcedev.attendance.mapper;

import com.marcedev.attendance.dto.UserDTO;
import com.marcedev.attendance.entities.Course;
import com.marcedev.attendance.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "organizationName",
            expression = "java(user.getOrganization() != null ? user.getOrganization().getName() : null)")
    @Mapping(target = "organizationId",
            expression = "java(user.getOrganization() != null ? user.getOrganization().getId() : null)")
    @Mapping(target = "courses", expression = "java(mapCourses(user.getCourses()))")
    UserDTO toDTO(User user);

    List<UserDTO> toDTOList(List<User> users);

    // 🔥 Este método resuelve el error
    default List<String> mapCourses(Set<Course> courses) {
        if (courses == null) return List.of();
        return courses.stream()
                .map(Course::getName)
                .collect(Collectors.toList());
    }
}
