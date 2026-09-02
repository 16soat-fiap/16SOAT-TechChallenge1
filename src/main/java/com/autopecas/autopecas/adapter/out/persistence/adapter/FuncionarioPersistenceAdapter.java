package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.entity.FuncionarioJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.mapper.FuncionarioJpaMapper;
import com.autopecas.autopecas.adapter.out.persistence.repository.FuncionarioJpaRepository;
import com.autopecas.autopecas.application.port.out.FuncionarioRepositorio;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.funcionario.Funcionario;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter de persistência do agregado Funcionario. */
@Component
public class FuncionarioPersistenceAdapter implements FuncionarioRepositorio {

    private final FuncionarioJpaRepository repository;
    private final FuncionarioJpaMapper mapper;

    public FuncionarioPersistenceAdapter(FuncionarioJpaRepository repository,
                                         FuncionarioJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Funcionario salvar(Funcionario funcionario) {
        FuncionarioJpaEntity entidade;
        if (funcionario.isNovo()) {
            entidade = mapper.novaEntidade(funcionario);
        } else {
            entidade = repository.findById(funcionario.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Funcionário não encontrado com ID: " + funcionario.getId()));
            mapper.aplicar(funcionario, entidade);
        }
        return mapper.paraDominio(repository.save(entidade));
    }

    @Override
    public Optional<Funcionario> porId(UUID id) {
        return repository.findById(id).map(mapper::paraDominio);
    }

    @Override
    public Optional<Funcionario> porEmail(String email) {
        return repository.findByEmail(email).map(mapper::paraDominio);
    }

    @Override
    public List<Funcionario> ativos() {
        return repository.findByAtivoTrue().stream().map(mapper::paraDominio).toList();
    }

    @Override
    public boolean existePorCpf(String cpf) {
        return repository.existsByCpf(cpf);
    }
}
