package com.autopecas.autopecas.mapper;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.ClientePF;
import com.autopecas.autopecas.domain.entity.ClientePJ;
import com.autopecas.autopecas.dto.cliente.ClienteResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    default ClienteResponseDTO toResponse(Cliente cliente) {
        if (cliente == null) {
            return null;
        }

        if (cliente instanceof ClientePF pf) {
            return toResponse(pf);
        } else if (cliente instanceof ClientePJ pj) {
            return toResponse(pj);
        }

        throw new IllegalArgumentException("Tipo de cliente desconhecido");
    }

    @Mapping(target = "cnpj", ignore = true)
    ClienteResponseDTO toResponse(ClientePF cliente);

    @Mapping(target = "cpf", ignore = true)
    ClienteResponseDTO toResponse(ClientePJ cliente);

}