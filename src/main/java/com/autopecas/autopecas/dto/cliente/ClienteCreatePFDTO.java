package com.autopecas.autopecas.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClienteCreatePFDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
        String nome,
        @Email
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