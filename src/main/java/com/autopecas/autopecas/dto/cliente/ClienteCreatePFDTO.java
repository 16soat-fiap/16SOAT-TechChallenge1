package com.autopecas.autopecas.dto.cliente;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ClienteCreatePFDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,
        String email,
        String telefone,
        boolean aceitaNotificacoes,
        @NotBlank(message = "CPF é obrigatório")
        String cpf,
        LocalDate dataNascimento,
        String rg,
        String genero
) {
}