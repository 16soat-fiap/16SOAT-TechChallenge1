package com.autopecas.autopecas.dto;

import java.util.UUID;

public record ClienteResponseDTO(
    UUID id,
    String nome,
    String cpf,
    String email,
    String telefone
) {



}
