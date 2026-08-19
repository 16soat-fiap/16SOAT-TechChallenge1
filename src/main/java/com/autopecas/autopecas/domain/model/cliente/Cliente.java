package com.autopecas.autopecas.domain.model.cliente;

import com.autopecas.autopecas.domain.enums.TipoCliente;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.vo.Endereco;

import java.util.UUID;

/**
 * Raiz de agregado abstrata para clientes.
 *
 * <p>Subclasses: ClientePF (CPF) e ClientePJ (CNPJ).
 *
 * <p>Regras:
 * <ul>
 *   <li>Nome é obrigatório.</li>
 *   <li>Cliente nunca é excluído — apenas desativado.</li>
 *   <li>O documento (CPF/CNPJ) é imutável após a criação.</li>
 * </ul>
 */
public abstract class Cliente {

    private final UUID id;
    private String nome;
    private String email;
    private String telefone;
    private boolean aceitaNotificacoes;
    private boolean ativo;
    private Endereco endereco;

    protected Cliente(UUID id, String nome, String email, String telefone,
                      boolean aceitaNotificacoes, boolean ativo, Endereco endereco) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessException("Nome do cliente é obrigatório");
        }
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.aceitaNotificacoes = aceitaNotificacoes;
        this.ativo = ativo;
        this.endereco = endereco;
    }

    /** CPF para PF, CNPJ para PJ — já normalizado. */
    public abstract String getDocumento();

    public abstract TipoCliente getTipo();

    /** Atualização parcial: campos nulos (ou nome em branco) são ignorados. */
    public void atualizarDados(String nome, String email, String telefone, Boolean aceitaNotificacoes) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome;
        }
        if (email != null) {
            this.email = email;
        }
        if (telefone != null) {
            this.telefone = telefone;
        }
        if (aceitaNotificacoes != null) {
            this.aceitaNotificacoes = aceitaNotificacoes;
        }
    }

    public void alterarEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public void desativar() {
        this.ativo = false;
    }

    /** Verdadeiro enquanto o cliente não tiver sido persistido (id atribuído pelo banco). */
    public boolean isNovo() {
        return id == null;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public boolean isAceitaNotificacoes() {
        return aceitaNotificacoes;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Endereco getEndereco() {
        return endereco;
    }
}
