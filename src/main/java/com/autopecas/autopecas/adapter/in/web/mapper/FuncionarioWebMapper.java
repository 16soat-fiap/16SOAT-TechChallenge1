package com.autopecas.autopecas.adapter.in.web.mapper;

import com.autopecas.autopecas.adapter.in.web.dto.funcionario.AtendenteCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.funcionario.FuncionarioResponseDTO;
import com.autopecas.autopecas.adapter.in.web.dto.funcionario.MecanicoCreateDTO;
import com.autopecas.autopecas.application.port.in.GestaoDeFuncionarios;
import com.autopecas.autopecas.application.port.in.view.FuncionarioView;
import org.mapstruct.Mapper;

/** Tradução entre os DTOs HTTP de funcionário e os commands/views da aplicação. */
@Mapper(componentModel = "spring")
public interface FuncionarioWebMapper {

    FuncionarioResponseDTO paraResposta(FuncionarioView view);

    GestaoDeFuncionarios.Cadastrar paraComando(MecanicoCreateDTO dto);

    GestaoDeFuncionarios.Cadastrar paraComando(AtendenteCreateDTO dto);
}
