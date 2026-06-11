package com.autopecas.autopecas.mapper;

import com.autopecas.autopecas.domain.entity.Orcamento;
import com.autopecas.autopecas.dto.orcamento.OrcamentoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrcamentoMapper {

    @Mapping(target = "status", expression = "java(orcamento.getStatus() != null ? orcamento.getStatus().name() : null)")
    OrcamentoResponseDTO toResponse(Orcamento orcamento);
}
