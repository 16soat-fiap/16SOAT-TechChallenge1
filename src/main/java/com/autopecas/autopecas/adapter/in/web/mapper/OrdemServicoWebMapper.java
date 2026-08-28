package com.autopecas.autopecas.adapter.in.web.mapper;

import com.autopecas.autopecas.adapter.in.web.dto.os.OrdemServicoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.os.OrdemServicoResponseDTO;
import com.autopecas.autopecas.application.port.in.GestaoDeOrdensServico;
import com.autopecas.autopecas.application.port.in.view.OrdemServicoView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/** Tradução entre os DTOs HTTP de OS e os commands/views da aplicação. */
@Mapper(componentModel = "spring")
public interface OrdemServicoWebMapper {

    @Mapping(source = "criadoEm", target = "createdAt")
    OrdemServicoResponseDTO paraResposta(OrdemServicoView view);

    /**
     * Itens da abertura. O comando completo é montado no controller, que é quem tem acesso ao
     * e-mail do atendente autenticado — aqui só viajam as listas.
     */
    List<GestaoDeOrdensServico.Abrir.ItemServico> paraItensServico(
            List<OrdemServicoCreateDTO.ItemServicoDTO> itens);

    List<GestaoDeOrdensServico.Abrir.ItemPeca> paraItensPeca(
            List<OrdemServicoCreateDTO.ItemPecaDTO> itens);
}
