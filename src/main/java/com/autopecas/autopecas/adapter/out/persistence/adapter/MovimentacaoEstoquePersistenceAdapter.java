package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.mapper.MovimentacaoEstoqueJpaMapper;
import com.autopecas.autopecas.adapter.out.persistence.repository.MovimentacaoEstoqueJpaRepository;
import com.autopecas.autopecas.application.port.out.MovimentacaoEstoqueRepositorio;
import com.autopecas.autopecas.domain.model.estoque.MovimentacaoEstoque;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Adapter de persistência do livro de movimentações. Somente-inserção. */
@Component
public class MovimentacaoEstoquePersistenceAdapter implements MovimentacaoEstoqueRepositorio {

    private final MovimentacaoEstoqueJpaRepository repository;
    private final MovimentacaoEstoqueJpaMapper mapper;

    public MovimentacaoEstoquePersistenceAdapter(MovimentacaoEstoqueJpaRepository repository,
                                                 MovimentacaoEstoqueJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacao) {
        return mapper.paraDominio(repository.save(mapper.novaEntidade(movimentacao)));
    }

    @Override
    public List<MovimentacaoEstoque> daPeca(UUID pecaId) {
        return repository.findByPecaIdOrderByCreatedAtDesc(pecaId).stream()
                .map(mapper::paraDominio).toList();
    }

    @Override
    public List<MovimentacaoEstoque> daOrdemServico(UUID ordemServicoId) {
        return repository.findByOrdemServicoId(ordemServicoId).stream()
                .map(mapper::paraDominio).toList();
    }
}
