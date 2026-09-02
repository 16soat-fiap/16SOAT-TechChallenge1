package com.autopecas.autopecas.application.port.in;

import com.autopecas.autopecas.application.port.in.view.FuncionarioView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Inbound port do agregado Funcionario. A matrícula é gerada, nunca informada. */
public interface GestaoDeFuncionarios {

    List<FuncionarioView> listarAtivos();

    FuncionarioView porId(UUID id);

    FuncionarioView cadastrarMecanico(Cadastrar comando);

    FuncionarioView cadastrarAtendente(Cadastrar comando);

    void desativar(UUID id);

    record Cadastrar(
            String nome,
            String cpf,
            String email,
            String telefone,
            LocalDate dataNascimento
    ) {
    }
}
