package com.autopecas.autopecas.dto.cliente;

import com.autopecas.autopecas.domain.valueobject.CPF;

import java.util.UUID;

public record ClienteResponseDTO(
        UUID id,
        String nome,
        String documento,
        String email,
        String telefone
) {
}
