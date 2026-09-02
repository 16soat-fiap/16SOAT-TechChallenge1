package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.MovimentacaoEstoqueJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repositório Spring Data das movimentações de estoque. */
public interface MovimentacaoEstoqueJpaRepository
        extends JpaRepository<MovimentacaoEstoqueJpaEntity, UUID> {

    List<MovimentacaoEstoqueJpaEntity> findByPecaIdOrderByCreatedAtDesc(UUID pecaId);

    List<MovimentacaoEstoqueJpaEntity> findByOrdemServicoId(UUID ordemServicoId);
}
