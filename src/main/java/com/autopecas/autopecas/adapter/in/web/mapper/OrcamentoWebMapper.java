package com.autopecas.autopecas.adapter.in.web.mapper;

import com.autopecas.autopecas.adapter.in.web.dto.orcamento.OrcamentoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.orcamento.OrcamentoResponseDTO;
import com.autopecas.autopecas.application.port.in.GestaoDeOrcamentos;
import com.autopecas.autopecas.application.port.in.view.OrcamentoView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Tradução entre os DTOs HTTP de orçamento e os commands/views da aplicação. */
@Mapper(componentModel = "spring")
public interface OrcamentoWebMapper {

    @Mapping(source = "criadoEm", target = "createdAt")
    OrcamentoResponseDTO paraResposta(OrcamentoView view);

    GestaoDeOrcamentos.Criar paraComando(OrcamentoCreateDTO dto);

    GestaoDeOrcamentos.Criar.ItemServico paraComando(OrcamentoCreateDTO.ItemServicoDTO dto);

    GestaoDeOrcamentos.Criar.ItemPeca paraComando(OrcamentoCreateDTO.ItemPecaDTO dto);
}
