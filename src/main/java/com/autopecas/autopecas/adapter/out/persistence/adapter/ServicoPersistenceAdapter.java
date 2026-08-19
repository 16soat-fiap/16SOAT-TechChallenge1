package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.entity.ServicoJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.mapper.ServicoJpaMapper;
import com.autopecas.autopecas.adapter.out.persistence.repository.ServicoJpaRepository;
import com.autopecas.autopecas.application.port.out.ServicoRepositorio;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.estoque.Servico;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter de persistência do catálogo de serviços. */
@Component
public class ServicoPersistenceAdapter implements ServicoRepositorio {

    private final ServicoJpaRepository repository;
    private final ServicoJpaMapper mapper;

    public ServicoPersistenceAdapter(ServicoJpaRepository repository, ServicoJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Servico salvar(Servico servico) {
        ServicoJpaEntity entidade;
        if (servico.isNovo()) {
            entidade = mapper.novaEntidade(servico);
        } else {
            entidade = repository.findById(servico.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Serviço não encontrado, ID: " + servico.getId()));
            mapper.aplicar(servico, entidade);
        }
        return mapper.paraDominio(repository.save(entidade));
    }

    @Override
    public Optional<Servico> porId(UUID id) {
        return repository.findById(id).map(mapper::paraDominio);
    }

    @Override
    public List<Servico> ativos() {
        return repository.findByAtivoTrue().stream().map(mapper::paraDominio).toList();
    }
}
