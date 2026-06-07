package com.autopecas.autopecas.dto.os;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record OrdemServicoCreateDTO(
        @NotBlank(message = "Cliente é obrigatório")
        UUID clienteId,

        @NotBlank(message = "Veículo é obrigatório")
        UUID veiculoId,

        @NotBlank(message = "Queixa do cliente é obrigatória")
        String queixaCliente,
        String observacoesEntrada
) {
}
