package com.autopecas.autopecas.domain.model.funcionario;

import com.autopecas.autopecas.domain.enums.TipoFuncionario;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.vo.Endereco;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Raiz de agregado abstrata para os funcionários da oficina.
 *
 * <p>Subclasses: Mecanico (executa serviços) e Atendente (abre e entrega OS).
 *
 * <p>A matrícula é única, imutável e funciona como crachá.
 *
 * <p>Nota: o CPF é mantido como String simples (sem o Value Object) porque a versão
 * anterior ao refactor não validava CPF de funcionário — trocar por CPF passaria a
 * rejeitar cadastros que hoje são aceitos.
 */
public abstract class Funcionario {

    private final UUID id;
    private final String matricula;
    private final String cpf;
    private String nome;
    private String email;
    private String telefone;
    private LocalDate dataNascimento;
    private boolean ativo;
    private Endereco endereco;

    protected Funcionario(UUID id, String matricula, String cpf, String nome, String email,
                          String telefone, LocalDate dataNascimento, boolean ativo, Endereco endereco) {
        if (matricula == null || matricula.isBlank()) {
            throw new BusinessException("Matrícula do funcionário é obrigatória");
        }
        if (cpf == null || cpf.isBlank()) {
            throw new BusinessException("CPF do funcionário é obrigatório");
        }
        if (nome == null || nome.isBlank()) {
            throw new BusinessException("Nome do funcionário é obrigatório");
        }
        this.id = id;
        this.matricula = matricula;
        this.cpf = cpf;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.dataNascimento = dataNascimento;
        this.ativo = ativo;
        this.endereco = endereco;
    }

    public abstract TipoFuncionario getTipo();

    /** Identificação para históricos, no formato "Carlos Silva (MEC-0042)". */
    public String getIdentificacao() {
        return String.format("%s (%s)", nome, matricula);
    }

    /** Rótulo usado no campo alteradoPor do histórico de status. */
    public String getIdentificacaoComTipo() {
        return getIdentificacao() + " — " + getTipo();
    }

    public void atualizarContato(String email, String telefone) {
        if (email != null) {
            this.email = email;
        }
        if (telefone != null) {
            this.telefone = telefone;
        }
    }

    public void desativar() {
        this.ativo = false;
    }

    public boolean isNovo() {
        return id == null;
    }

    public UUID getId() {
        return id;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getCpf() {
        return cpf;
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

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Endereco getEndereco() {
        return endereco;
    }
}
