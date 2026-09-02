package com.autopecas.autopecas.domain.model.cliente;

import com.autopecas.autopecas.domain.enums.Genero;
import com.autopecas.autopecas.domain.enums.TipoCliente;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.vo.CPF;
import com.autopecas.autopecas.domain.vo.Endereco;

import java.time.LocalDate;
import java.util.UUID;

/** Cliente Pessoa Física, identificado por CPF. */
public final class ClientePF extends Cliente {

    private final CPF cpf;
    private final LocalDate dataNascimento;
    private final String rg;
    private final Genero genero;
    private final String profissao;

    private ClientePF(UUID id, String nome, String email, String telefone,
                      boolean aceitaNotificacoes, boolean ativo, Endereco endereco,
                      CPF cpf, LocalDate dataNascimento, String rg, Genero genero, String profissao) {
        super(id, nome, email, telefone, aceitaNotificacoes, ativo, endereco);
        if (cpf == null) {
            throw new BusinessException("CPF é obrigatório para cliente PF");
        }
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.rg = rg;
        this.genero = genero;
        this.profissao = profissao;
    }

    /** Novo cliente PF, ainda sem id. */
    public static ClientePF criar(String nome, String email, String telefone, boolean aceitaNotificacoes,
                                  CPF cpf, LocalDate dataNascimento, String rg, Genero genero) {
        return new ClientePF(null, nome, email, telefone, aceitaNotificacoes, true, null,
                cpf, dataNascimento, rg, genero, null);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static ClientePF reconstituir(UUID id, String nome, String email, String telefone,
                                         boolean aceitaNotificacoes, boolean ativo, Endereco endereco,
                                         CPF cpf, LocalDate dataNascimento, String rg,
                                         Genero genero, String profissao) {
        return new ClientePF(id, nome, email, telefone, aceitaNotificacoes, ativo, endereco,
                cpf, dataNascimento, rg, genero, profissao);
    }

    @Override
    public String getDocumento() {
        return cpf.valor();
    }

    @Override
    public TipoCliente getTipo() {
        return TipoCliente.PF;
    }

    public CPF getCpf() {
        return cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getRg() {
        return rg;
    }

    public Genero getGenero() {
        return genero;
    }

    public String getProfissao() {
        return profissao;
    }
}
