package com.autopecas.autopecas.application.port.in;

import com.autopecas.autopecas.application.port.in.view.ClienteView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port do agregado Cliente.
 *
 * <p>Os commands são records puros: nenhuma anotação de validação do Jakarta atravessa a
 * fronteira — a validação de formato fica nos DTOs do adapter web.
 */
public interface GestaoDeClientes {

    List<ClienteView> listarAtivos();

    ClienteView porId(UUID id);

    /** Busca por CPF (11 dígitos) ou CNPJ (14 dígitos). */
    ClienteView porDocumento(String documento);

    ClienteView cadastrarPF(CadastrarPF comando);

    ClienteView cadastrarPJ(CadastrarPJ comando);

    ClienteView atualizar(UUID id, AtualizarDados comando);

    void desativar(UUID id);

    record CadastrarPF(
            String nome,
            String email,
            String telefone,
            boolean aceitaNotificacoes,
            String cpf,
            LocalDate dataNascimento,
            String rg,
            String genero
    ) {
    }

    record CadastrarPJ(
            String nome,
            String email,
            String telefone,
            boolean aceitaNotificacoes,
            String cnpj,
            String razaoSocial,
            String inscricaoEstadual
    ) {
    }

    /** Atualização parcial: campos nulos são ignorados pelo domínio. */
    record AtualizarDados(
            String nome,
            String email,
            String telefone,
            Boolean aceitaNotificacoes
    ) {
    }
}
