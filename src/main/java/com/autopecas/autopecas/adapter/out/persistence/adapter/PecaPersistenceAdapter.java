package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.entity.PecaJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.mapper.PecaJpaMapper;
import com.autopecas.autopecas.adapter.out.persistence.repository.PecaJpaRepository;
import com.autopecas.autopecas.application.port.out.PecaRepositorio;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.estoque.Peca;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter de persistência do agregado Peca. */
@Component
public class PecaPersistenceAdapter implements PecaRepositorio {

    private final PecaJpaRepository repository;
    private final PecaJpaMapper mapper;

    public PecaPersistenceAdapter(PecaJpaRepository repository, PecaJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Peca salvar(Peca peca) {
        PecaJpaEntity entidade;
        if (peca.isNovo()) {
            entidade = mapper.novaEntidade(peca);
        } else {
            entidade = repository.findById(peca.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Peça não encontrada. ID: " + peca.getId()));
            mapper.aplicar(peca, entidade);
        }
        return mapper.paraDominio(repository.save(entidade));
    }

    @Override
    public Optional<Peca> porId(UUID id) {
        return repository.findById(id).map(mapper::paraDominio);
    }

    @Override
    public Optional<Peca> porCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::paraDominio);
    }

    @Override
    public List<Peca> ativas() {
        return repository.findByAtivoTrue().stream().map(mapper::paraDominio).toList();
    }

    @Override
    public List<Peca> comEstoqueBaixo() {
        return repository.findEstoqueBaixo().stream().map(mapper::paraDominio).toList();
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return repository.existsByCodigo(codigo);
    }
}
