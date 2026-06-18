package com.autopecas.autopecas.mapper;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.dto.cliente.ClienteResponseDTO;
import org.mapstruct.Mapper;

@Mapper( componentModel = "spring")
public interface ClienteMapper {
    ClienteResponseDTO  toResponse(Cliente cliente);
}
