package com.autopecas.autopecas.adapter.in.web.dto.funcionario;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AtendenteCreateDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "CPF é obrigatório")
        String cpf,
        String email,
        String telefone,
        LocalDate dataNascimento
) {
}
