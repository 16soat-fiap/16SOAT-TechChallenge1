package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.OrcamentoJpaEntity;
import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repositório Spring Data de orçamentos. */
public interface OrcamentoJpaRepository extends JpaRepository<OrcamentoJpaEntity, UUID> {

    List<OrcamentoJpaEntity> findByOrdemServicoId(UUID ordemServicoId);

    boolean existsByOrdemServicoIdAndStatus(UUID ordemServicoId, StatusOrcamento status);

    /**
     * Maior versão já usada na OS, ou nulo se ainda não houver orçamento. A unicidade final é
     * garantida pela constraint uk_orcamento_os_versao.
     */
    @Query("SELECT MAX(o.versao) FROM OrcamentoJpaEntity o WHERE o.ordemServicoId = :ordemServicoId")
    Optional<Integer> maiorVersao(@Param("ordemServicoId") UUID ordemServicoId);
}
