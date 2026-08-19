package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.entity.OrcamentoJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.mapper.OrcamentoJpaMapper;
import com.autopecas.autopecas.adapter.out.persistence.repository.OrcamentoJpaRepository;
import com.autopecas.autopecas.application.port.out.OrcamentoRepositorio;
import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.orcamento.Orcamento;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter de persistência do agregado Orcamento, incluindo seus itens. */
@Component
public class OrcamentoPersistenceAdapter implements OrcamentoRepositorio {

    private final OrcamentoJpaRepository repository;
    private final OrcamentoJpaMapper mapper;

    public OrcamentoPersistenceAdapter(OrcamentoJpaRepository repository, OrcamentoJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Orcamento salvar(Orcamento orcamento) {
        OrcamentoJpaEntity entidade;
        if (orcamento.isNovo()) {
            entidade = mapper.novaEntidade(orcamento);
        } else {
            entidade = repository.findById(orcamento.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Orcamento não encontrado"));
            mapper.aplicar(orcamento, entidade);
        }
        return mapper.paraDominio(repository.save(entidade));
    }

    @Override
    public Optional<Orcamento> porId(UUID id) {
        return repository.findById(id).map(mapper::paraDominio);
    }

    @Override
    public List<Orcamento> daOrdemServico(UUID ordemServicoId) {
        return repository.findByOrdemServicoId(ordemServicoId).stream()
                .map(mapper::paraDominio).toList();
    }

    @Override
    public boolean existeAprovadoParaOrdemServico(UUID ordemServicoId) {
        return repository.existsByOrdemServicoIdAndStatus(ordemServicoId, StatusOrcamento.APROVADA);
    }

    @Override
    public int proximaVersao(UUID ordemServicoId) {
        return repository.maiorVersao(ordemServicoId).orElse(0) + 1;
    }
}
