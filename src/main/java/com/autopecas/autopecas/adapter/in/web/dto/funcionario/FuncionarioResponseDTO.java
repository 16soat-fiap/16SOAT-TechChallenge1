package com.autopecas.autopecas.adapter.in.web.dto.funcionario;

import java.util.UUID;

public record FuncionarioResponseDTO(
        UUID id,
        String matricula,
        String nome,
        String email,
        String telefone,
        String tipo,
        Boolean ativo
) {
}
