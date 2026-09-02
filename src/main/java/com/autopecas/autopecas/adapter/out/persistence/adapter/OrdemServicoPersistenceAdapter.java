package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.entity.OrdemServicoJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.mapper.OrdemServicoJpaMapper;
import com.autopecas.autopecas.adapter.out.persistence.repository.OrdemServicoJpaRepository;
import com.autopecas.autopecas.application.port.out.OrdemServicoRepositorio;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.os.OrdemServico;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Adapter de persistência do agregado OrdemServico, incluindo seus itens. */
@Component
public class OrdemServicoPersistenceAdapter implements OrdemServicoRepositorio {

    private final OrdemServicoJpaRepository repository;
    private final OrdemServicoJpaMapper mapper;

    public OrdemServicoPersistenceAdapter(OrdemServicoJpaRepository repository,
                                          OrdemServicoJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OrdemServico salvar(OrdemServico ordemServico) {
        OrdemServicoJpaEntity entidade;
        if (ordemServico.isNovo()) {
            entidade = mapper.novaEntidade(ordemServico);
        } else {
            // porIdParaAtualizacao aplica OPTIMISTIC_FORCE_INCREMENT: é o que devolve ao agregado
            // destacado a proteção contra lost update que o @Version dava quando a entidade JPA
            // era o próprio domínio.
            entidade = repository.porIdParaAtualizacao(ordemServico.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ordem de serviço não encontrada com ID: " + ordemServico.getId()));
            mapper.aplicar(ordemServico, entidade);
        }
        return mapper.paraDominio(repository.save(entidade));
    }

    @Override
    public Optional<OrdemServico> porId(UUID id) {
        return repository.findById(id).map(mapper::paraDominio);
    }

    @Override
    public Optional<OrdemServico> porNumero(String numero) {
        return repository.findByNumero(numero).map(mapper::paraDominio);
    }

    @Override
    public boolean existePorId(UUID id) {
        return repository.existsById(id);
    }
}
