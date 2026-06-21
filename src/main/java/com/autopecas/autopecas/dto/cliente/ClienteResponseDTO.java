package com.autopecas.autopecas.dto.cliente;

import java.util.UUID;

public record ClienteResponseDTO(
        UUID id,
        String nome,
        String cpf,
        String cnpj,
        String email,
        String telefone
) {
}
