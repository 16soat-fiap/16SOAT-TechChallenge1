package com.autopecas.autopecas.mapper;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.dto.cliente.ClienteResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper( componentModel = "spring")
public interface ClienteMapper {
    @Mapping(target = "tipo", expression = "java(cliente.getTipo().name())")
    @Mapping(source = "documento", target = "documento")
    ClienteResponseDTO  toResponse(Cliente cliente);
}
