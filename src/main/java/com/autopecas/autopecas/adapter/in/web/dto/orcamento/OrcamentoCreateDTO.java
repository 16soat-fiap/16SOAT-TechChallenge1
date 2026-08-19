package com.autopecas.autopecas.adapter.in.web.dto.orcamento;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrcamentoCreateDTO(
        List<ItemServicoDTO> itensServico,
        List<ItemPecaDTO> itensPeca,
        String condicoesPagamento,
        Integer prazoExecucaoDias,
        LocalDate dataValidade,
        String observacoes
) {

    public record ItemServicoDTO(
            UUID servicoId,
            Integer quantidade
    ){
    }

    public record ItemPecaDTO(
            UUID pecaId,
            Integer quantidade
    ){
    }
}
