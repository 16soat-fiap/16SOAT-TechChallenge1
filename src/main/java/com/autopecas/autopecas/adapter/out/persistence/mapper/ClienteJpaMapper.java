package com.autopecas.autopecas.adapter.out.persistence.mapper;

import com.autopecas.autopecas.adapter.out.persistence.entity.ClienteJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.entity.ClientePFJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.entity.ClientePJJpaEntity;
import com.autopecas.autopecas.domain.model.cliente.Cliente;
import com.autopecas.autopecas.domain.model.cliente.ClientePF;
import com.autopecas.autopecas.domain.model.cliente.ClientePJ;
import com.autopecas.autopecas.domain.vo.CNPJ;
import com.autopecas.autopecas.domain.vo.CPF;
import org.springframework.stereotype.Component;

/**
 * Conversão entre o agregado Cliente e sua entidade JPA.
 *
 * <p>Escrito à mão porque a re-hidratação passa pelos factory methods do domínio, que validam
 * invariantes e têm construtor privado.
 */
@Component
public class ClienteJpaMapper {

    public Cliente paraDominio(ClienteJpaEntity entidade) {
        if (entidade instanceof ClientePFJpaEntity pf) {
            return ClientePF.reconstituir(pf.getId(), pf.getNome(), pf.getEmail(), pf.getTelefone(),
                    Boolean.TRUE.equals(pf.getAceitaNotificacoes()), Boolean.TRUE.equals(pf.getAtivo()),
                    EnderecoJpaMapper.paraDominio(pf.getEndereco()), new CPF(pf.getCpf()),
                    pf.getDataNascimento(), pf.getRg(), pf.getGenero(), pf.getProfissao());
        }
        if (entidade instanceof ClientePJJpaEntity pj) {
            return ClientePJ.reconstituir(pj.getId(), pj.getNome(), pj.getEmail(), pj.getTelefone(),
                    Boolean.TRUE.equals(pj.getAceitaNotificacoes()), Boolean.TRUE.equals(pj.getAtivo()),
                    EnderecoJpaMapper.paraDominio(pj.getEndereco()), new CNPJ(pj.getCnpj()),
                    pj.getRazaoSocial(), pj.getInscricaoEstadual(), pj.getInscricaoMunicipal(),
                    pj.getContatoResponsavel());
        }
        throw new IllegalStateException(
                "Tipo de cliente não suportado: " + entidade.getClass().getName());
    }

    /** Cria a entidade de um cliente ainda não persistido. */
    public ClienteJpaEntity novaEntidade(Cliente cliente) {
        if (cliente instanceof ClientePF pf) {
            ClientePFJpaEntity entidade = new ClientePFJpaEntity();
            entidade.setCpf(pf.getCpf().valor());
            entidade.setDataNascimento(pf.getDataNascimento());
            entidade.setRg(pf.getRg());
            entidade.setGenero(pf.getGenero());
            entidade.setProfissao(pf.getProfissao());
            aplicarComuns(pf, entidade);
            return entidade;
        }
        if (cliente instanceof ClientePJ pj) {
            ClientePJJpaEntity entidade = new ClientePJJpaEntity();
            entidade.setCnpj(pj.getCnpj().valor());
            entidade.setRazaoSocial(pj.getRazaoSocial());
            entidade.setInscricaoEstadual(pj.getInscricaoEstadual());
            entidade.setInscricaoMunicipal(pj.getInscricaoMunicipal());
            entidade.setContatoResponsavel(pj.getContatoResponsavel());
            aplicarComuns(pj, entidade);
            return entidade;
        }
        throw new IllegalStateException(
                "Tipo de cliente não suportado: " + cliente.getClass().getName());
    }

    /**
     * Aplica o estado do agregado sobre uma entidade já carregada. Documento e datas de
     * auditoria não são tocados: são imutáveis ou gerenciados pelo Hibernate.
     */
    public void aplicar(Cliente cliente, ClienteJpaEntity entidade) {
        aplicarComuns(cliente, entidade);
        if (cliente instanceof ClientePF pf && entidade instanceof ClientePFJpaEntity entidadePf) {
            entidadePf.setDataNascimento(pf.getDataNascimento());
            entidadePf.setRg(pf.getRg());
            entidadePf.setGenero(pf.getGenero());
            entidadePf.setProfissao(pf.getProfissao());
        } else if (cliente instanceof ClientePJ pj && entidade instanceof ClientePJJpaEntity entidadePj) {
            entidadePj.setRazaoSocial(pj.getRazaoSocial());
            entidadePj.setInscricaoEstadual(pj.getInscricaoEstadual());
            entidadePj.setInscricaoMunicipal(pj.getInscricaoMunicipal());
            entidadePj.setContatoResponsavel(pj.getContatoResponsavel());
        }
    }

    private void aplicarComuns(Cliente cliente, ClienteJpaEntity entidade) {
        entidade.setNome(cliente.getNome());
        entidade.setEmail(cliente.getEmail());
        entidade.setTelefone(cliente.getTelefone());
        entidade.setAceitaNotificacoes(cliente.isAceitaNotificacoes());
        entidade.setAtivo(cliente.isAtivo());
        entidade.setEndereco(EnderecoJpaMapper.paraEntidade(cliente.getEndereco()));
    }
}
