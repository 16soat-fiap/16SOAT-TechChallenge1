package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.HistoricoStatusOSJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repositório Spring Data do histórico de status. */
public interface HistoricoStatusOSJpaRepository
        extends JpaRepository<HistoricoStatusOSJpaEntity, Long> {

    List<HistoricoStatusOSJpaEntity> findByOrdemServicoIdOrderByCreatedAtAsc(UUID ordemServicoId);
}
