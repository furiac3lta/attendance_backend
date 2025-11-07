package com.marcedev.attendance.mapper;

import com.marcedev.attendance.dto.UserDTO;
import com.marcedev.attendance.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mappings({
            @Mapping(source = "organization.id", target = "organizationId", defaultValue = "0L"),
            @Mapping(source = "organization.name", target = "organizationName", defaultValue = "Sin organización")
    })
    UserDTO toDTO(User user);

    @Mappings({
            @Mapping(target = "organization.id", source = "organizationId"),
            @Mapping(target = "organization.name", source = "organizationName")
    })
    User toEntity(UserDTO dto);
}
