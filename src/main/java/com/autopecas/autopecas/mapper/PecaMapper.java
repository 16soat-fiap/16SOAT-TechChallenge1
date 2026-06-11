package com.autopecas.autopecas.mapper;

import com.autopecas.autopecas.domain.entity.Peca;
import com.autopecas.autopecas.dto.peca.PecaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PecaMapper {

    @Mapping(target = "estoqueBaixo", expression = "java(peca.estoqueBaixo())")
    PecaResponseDTO toResponse(Peca peca);
}
