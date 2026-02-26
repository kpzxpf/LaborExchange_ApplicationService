package com.vlz.laborexchange_applicationservice.mapper;

import com.vlz.laborexchange_applicationservice.dto.ApplicationRequestDto;
import com.vlz.laborexchange_applicationservice.dto.ApplicationResponseDto;
import com.vlz.laborexchange_applicationservice.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(source = "status.code", target = "statusName")
    ApplicationResponseDto toDto(Application entity);
}