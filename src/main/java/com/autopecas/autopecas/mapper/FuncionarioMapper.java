package com.autopecas.autopecas.mapper;

import com.autopecas.autopecas.domain.entity.Funcionario;
import com.autopecas.autopecas.dto.funcionario.FuncionarioResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FuncionarioMapper {
    @Mapping(target = "tipo", expression = "java(funcionario.getTipo().name())")
    FuncionarioResponseDTO toResponse(Funcionario funcionario);
}
