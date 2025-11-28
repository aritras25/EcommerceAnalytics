package org.example.mapper;

import org.example.dto.UserEventInputRequest;
import org.example.entity.UserEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserEventMapper {
    UserEvent toEntity(UserEventInputRequest userEventInputRequest);
    UserEventInputRequest toDto(UserEvent userEvent);
}
