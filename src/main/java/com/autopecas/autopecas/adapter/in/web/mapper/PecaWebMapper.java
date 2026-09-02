package com.autopecas.autopecas.adapter.in.web.mapper;

import com.autopecas.autopecas.adapter.in.web.dto.peca.MovimentacaoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.peca.MovimentacaoResponseDTO;
import com.autopecas.autopecas.adapter.in.web.dto.peca.PecaCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.peca.PecaResponseDTO;
import com.autopecas.autopecas.application.port.in.GestaoDePecas;
import com.autopecas.autopecas.application.port.in.view.MovimentacaoView;
import com.autopecas.autopecas.application.port.in.view.PecaView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Tradução entre os DTOs HTTP de peça e os commands/views da aplicação. */
@Mapper(componentModel = "spring")
public interface PecaWebMapper {

    @Mapping(source = "criadoEm", target = "createdAt")
    @Mapping(source = "atualizadoEm", target = "updatedAt")
    PecaResponseDTO paraResposta(PecaView view);

    @Mapping(source = "criadoEm", target = "createdAt")
    MovimentacaoResponseDTO paraResposta(MovimentacaoView view);

    GestaoDePecas.Cadastrar paraComando(PecaCreateDTO dto);

    GestaoDePecas.AtualizarDados paraComandoDeAtualizacao(PecaCreateDTO dto);

    GestaoDePecas.RegistrarMovimentacao paraComando(MovimentacaoCreateDTO dto);
}
