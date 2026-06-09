package com.autopecas.autopecas.mapper;

import com.autopecas.autopecas.domain.entity.Veiculo;
import com.autopecas.autopecas.dto.veiculo.VeiculoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VeiculoMapper {
    @Mapping(source = "cliente.id", target = "clienteId")
    VeiculoResponseDTO toResponse(Veiculo veiculo);
}
