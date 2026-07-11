package com.nalitech.modules.user.mapper;

import com.nalitech.modules.user.dto.UserDtos.UserResponse;
import com.nalitech.modules.user.entity.Role;
import com.nalitech.modules.user.entity.RoleName;
import com.nalitech.modules.user.entity.User;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(mapRoles(user))")
    UserResponse toResponse(User user);

    default Set<RoleName> mapRoles(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
