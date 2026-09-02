package com.autopecas.autopecas.domain.model.cliente;

import com.autopecas.autopecas.domain.enums.TipoCliente;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.vo.CNPJ;
import com.autopecas.autopecas.domain.vo.Endereco;

import java.util.UUID;

/** Cliente Pessoa Jurídica, identificado por CNPJ. */
public final class ClientePJ extends Cliente {

    private final CNPJ cnpj;
    private final String razaoSocial;
    private final String inscricaoEstadual;
    private final String inscricaoMunicipal;
    private final String contatoResponsavel;

    private ClientePJ(UUID id, String nome, String email, String telefone,
                      boolean aceitaNotificacoes, boolean ativo, Endereco endereco,
                      CNPJ cnpj, String razaoSocial, String inscricaoEstadual,
                      String inscricaoMunicipal, String contatoResponsavel) {
        super(id, nome, email, telefone, aceitaNotificacoes, ativo, endereco);
        if (cnpj == null) {
            throw new BusinessException("CNPJ é obrigatório para cliente PJ");
        }
        if (razaoSocial == null || razaoSocial.isBlank()) {
            throw new BusinessException("Razão social é obrigatória para cliente PJ");
        }
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.inscricaoEstadual = inscricaoEstadual;
        this.inscricaoMunicipal = inscricaoMunicipal;
        this.contatoResponsavel = contatoResponsavel;
    }

    /** Novo cliente PJ, ainda sem id. */
    public static ClientePJ criar(String nome, String email, String telefone, boolean aceitaNotificacoes,
                                  CNPJ cnpj, String razaoSocial, String inscricaoEstadual) {
        return new ClientePJ(null, nome, email, telefone, aceitaNotificacoes, true, null,
                cnpj, razaoSocial, inscricaoEstadual, null, null);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static ClientePJ reconstituir(UUID id, String nome, String email, String telefone,
                                         boolean aceitaNotificacoes, boolean ativo, Endereco endereco,
                                         CNPJ cnpj, String razaoSocial, String inscricaoEstadual,
                                         String inscricaoMunicipal, String contatoResponsavel) {
        return new ClientePJ(id, nome, email, telefone, aceitaNotificacoes, ativo, endereco,
                cnpj, razaoSocial, inscricaoEstadual, inscricaoMunicipal, contatoResponsavel);
    }

    @Override
    public String getDocumento() {
        return cnpj.valor();
    }

    @Override
    public TipoCliente getTipo() {
        return TipoCliente.PJ;
    }

    public CNPJ getCnpj() {
        return cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getInscricaoEstadual() {
        return inscricaoEstadual;
    }

    public String getInscricaoMunicipal() {
        return inscricaoMunicipal;
    }

    public String getContatoResponsavel() {
        return contatoResponsavel;
    }
}
