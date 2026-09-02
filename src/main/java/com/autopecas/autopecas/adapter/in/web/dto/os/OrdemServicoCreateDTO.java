package com.autopecas.autopecas.adapter.in.web.dto.os;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

/**
 * Dados de abertura de uma OS.
 *
 * <p>Serviços e peças são opcionais: registram o que já foi acordado na recepção, ao preço
 * vigente no catálogo. Não baixam estoque — a baixa acontece na aprovação do orçamento.
 */
public record OrdemServicoCreateDTO(
        @NotNull(message = "Cliente é obrigatório")
        UUID clienteId,

        @NotNull(message = "Veículo é obrigatório")
        UUID veiculoId,

        @NotBlank(message = "Queixa do cliente é obrigatória")
        String queixaCliente,

        String observacoesEntrada,

        Integer quilometragemEntrada,

        @Valid
        List<ItemServicoDTO> itensServico,

        @Valid
        List<ItemPecaDTO> itensPeca
) {

    /** Quantidade omitida assume 1. */
    public record ItemServicoDTO(
            @NotNull(message = "Serviço é obrigatório")
            UUID servicoId,

            @Positive(message = "Quantidade deve ser positiva")
            Integer quantidade
    ) {
    }

    public record ItemPecaDTO(
            @NotNull(message = "Peça é obrigatória")
            UUID pecaId,

            @Positive(message = "Quantidade deve ser positiva")
            Integer quantidade
    ) {
    }
}
