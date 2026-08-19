package com.autopecas.autopecas.adapter.in.web.mapper;

import com.autopecas.autopecas.adapter.in.web.dto.os.OrdemServicoResponseDTO;
import com.autopecas.autopecas.application.port.in.view.OrdemServicoView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Tradução da view de OS para o DTO de resposta HTTP. */
@Mapper(componentModel = "spring")
public interface OrdemServicoWebMapper {

    @Mapping(source = "criadoEm", target = "createdAt")
    OrdemServicoResponseDTO paraResposta(OrdemServicoView view);
}
