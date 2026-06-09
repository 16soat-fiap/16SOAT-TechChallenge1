package com.autopecas.autopecas.mapper;

import com.autopecas.autopecas.domain.entity.Servico;
import com.autopecas.autopecas.dto.servico.ServicoCreateDTO;
import com.autopecas.autopecas.dto.servico.ServicoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServicoMapper {
    ServicoResponseDTO toResponse(Servico servico);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Servico toEntity(ServicoCreateDTO dto);
}
