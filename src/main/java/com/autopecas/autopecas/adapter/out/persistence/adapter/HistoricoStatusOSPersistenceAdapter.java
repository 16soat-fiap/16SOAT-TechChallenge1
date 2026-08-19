package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.mapper.HistoricoStatusOSJpaMapper;
import com.autopecas.autopecas.adapter.out.persistence.repository.HistoricoStatusOSJpaRepository;
import com.autopecas.autopecas.application.port.out.HistoricoStatusOSRepositorio;
import com.autopecas.autopecas.domain.model.os.HistoricoStatusOS;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Adapter de persistência do histórico de status. Somente-inserção. */
@Component
public class HistoricoStatusOSPersistenceAdapter implements HistoricoStatusOSRepositorio {

    private final HistoricoStatusOSJpaRepository repository;
    private final HistoricoStatusOSJpaMapper mapper;

    public HistoricoStatusOSPersistenceAdapter(HistoricoStatusOSJpaRepository repository,
                                               HistoricoStatusOSJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public HistoricoStatusOS salvar(HistoricoStatusOS historico) {
        return mapper.paraDominio(repository.save(mapper.novaEntidade(historico)));
    }

    @Override
    public List<HistoricoStatusOS> daOrdemServico(UUID ordemServicoId) {
        return repository.findByOrdemServicoIdOrderByCreatedAtAsc(ordemServicoId).stream()
                .map(mapper::paraDominio).toList();
    }
}
