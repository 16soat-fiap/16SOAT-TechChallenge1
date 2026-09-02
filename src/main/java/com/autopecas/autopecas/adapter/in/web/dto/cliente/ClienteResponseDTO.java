package com.autopecas.autopecas.adapter.in.web.dto.cliente;


import java.util.UUID;

public record ClienteResponseDTO(
        UUID id,
        String nome,
        String documento,
        String email,
        String telefone
) {
}
