package com.autopecas.autopecas.adapter.out.persistence.mapper;

import com.autopecas.autopecas.adapter.out.persistence.entity.AtendenteJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.entity.FuncionarioJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.entity.MecanicoJpaEntity;
import com.autopecas.autopecas.domain.model.funcionario.Atendente;
import com.autopecas.autopecas.domain.model.funcionario.Funcionario;
import com.autopecas.autopecas.domain.model.funcionario.Mecanico;
import org.springframework.stereotype.Component;

/** Conversão entre o agregado Funcionario e sua entidade JPA. */
@Component
public class FuncionarioJpaMapper {

    public Funcionario paraDominio(FuncionarioJpaEntity entidade) {
        if (entidade instanceof MecanicoJpaEntity mecanico) {
            return Mecanico.reconstituir(mecanico.getId(), mecanico.getMatricula(), mecanico.getCpf(),
                    mecanico.getNome(), mecanico.getEmail(), mecanico.getTelefone(),
                    mecanico.getDataNascimento(), Boolean.TRUE.equals(mecanico.getAtivo()),
                    EnderecoJpaMapper.paraDominio(mecanico.getEndereco()));
        }
        if (entidade instanceof AtendenteJpaEntity atendente) {
            return Atendente.reconstituir(atendente.getId(), atendente.getMatricula(),
                    atendente.getCpf(), atendente.getNome(), atendente.getEmail(),
                    atendente.getTelefone(), atendente.getDataNascimento(),
                    Boolean.TRUE.equals(atendente.getAtivo()),
                    EnderecoJpaMapper.paraDominio(atendente.getEndereco()));
        }
        throw new IllegalStateException(
                "Tipo de funcionário não suportado: " + entidade.getClass().getName());
    }

    /** Cria a entidade de um funcionário ainda não persistido. */
    public FuncionarioJpaEntity novaEntidade(Funcionario funcionario) {
        FuncionarioJpaEntity entidade = switch (funcionario) {
            case Mecanico ignored -> new MecanicoJpaEntity();
            case Atendente ignored -> new AtendenteJpaEntity();
            default -> throw new IllegalStateException(
                    "Tipo de funcionário não suportado: " + funcionario.getClass().getName());
        };
        entidade.setMatricula(funcionario.getMatricula());
        entidade.setCpf(funcionario.getCpf());
        aplicar(funcionario, entidade);
        return entidade;
    }

    /** Aplica o estado do agregado sobre uma entidade já carregada. */
    public void aplicar(Funcionario funcionario, FuncionarioJpaEntity entidade) {
        entidade.setNome(funcionario.getNome());
        entidade.setEmail(funcionario.getEmail());
        entidade.setTelefone(funcionario.getTelefone());
        entidade.setDataNascimento(funcionario.getDataNascimento());
        entidade.setAtivo(funcionario.isAtivo());
        entidade.setEndereco(EnderecoJpaMapper.paraEntidade(funcionario.getEndereco()));
    }
}
