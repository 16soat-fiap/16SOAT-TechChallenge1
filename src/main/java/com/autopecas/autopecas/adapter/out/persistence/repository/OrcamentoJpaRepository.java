package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.OrcamentoJpaEntity;
import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repositório Spring Data de orçamentos. */
public interface OrcamentoJpaRepository extends JpaRepository<OrcamentoJpaEntity, UUID> {

    /**
     * Carrega o orçamento para escrita forçando o incremento da versão no commit.
     *
     * <p>O agregado de domínio é destacado da entidade, então o {@code @Version} sozinho nunca
     * conflitaria: o adapter recarrega a linha antes de gravar e enxergaria sempre a versão mais
     * nova. Com FORCE_INCREMENT, duas transações que aprovem o mesmo orçamento em paralelo
     * disputam a mesma versão e uma delas falha no commit.
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT o FROM OrcamentoJpaEntity o WHERE o.id = :id")
    Optional<OrcamentoJpaEntity> porIdParaAtualizacao(@Param("id") UUID id);

    List<OrcamentoJpaEntity> findByOrdemServicoId(UUID ordemServicoId);

    boolean existsByOrdemServicoIdAndStatus(UUID ordemServicoId, StatusOrcamento status);

    /**
     * Maior versão já usada na OS, ou nulo se ainda não houver orçamento. A unicidade final é
     * garantida pela constraint uk_orcamento_os_versao.
     */
    @Query("SELECT MAX(o.versao) FROM OrcamentoJpaEntity o WHERE o.ordemServicoId = :ordemServicoId")
    Optional<Integer> maiorVersao(@Param("ordemServicoId") UUID ordemServicoId);
}
