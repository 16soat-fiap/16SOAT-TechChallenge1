package com.autopecas.autopecas.mapper;

import com.autopecas.autopecas.dto.ClienteResponseDTO;
import com.autopecas.autopecas.entity.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    ClienteResponseDTO toResponse(Cliente cliente);

}
