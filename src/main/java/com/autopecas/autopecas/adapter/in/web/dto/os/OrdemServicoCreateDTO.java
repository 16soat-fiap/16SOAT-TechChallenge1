package com.autopecas.autopecas.adapter.in.web.dto.os;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrdemServicoCreateDTO(
        @NotNull(message = "Cliente é obrigatório")
        UUID clienteId,

        @NotNull(message = "Veículo é obrigatório")
        UUID veiculoId,

        @NotBlank(message = "Queixa do cliente é obrigatória")
        String queixaCliente,

        String observacoesEntrada,

        Integer quilometragemEntrada
) {
}
