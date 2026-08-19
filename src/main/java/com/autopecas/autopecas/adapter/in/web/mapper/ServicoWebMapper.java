package com.autopecas.autopecas.adapter.in.web.mapper;

import com.autopecas.autopecas.adapter.in.web.dto.servico.ServicoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.servico.ServicoResponseDTO;
import com.autopecas.autopecas.application.port.in.GestaoDeServicos;
import com.autopecas.autopecas.application.port.in.view.ServicoView;
import org.mapstruct.Mapper;

/** Tradução entre os DTOs HTTP de serviço e os commands/views da aplicação. */
@Mapper(componentModel = "spring")
public interface ServicoWebMapper {

    ServicoResponseDTO paraResposta(ServicoView view);

    GestaoDeServicos.DadosDoServico paraComando(ServicoCreateDTO dto);
}
