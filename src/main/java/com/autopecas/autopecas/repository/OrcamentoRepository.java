package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.Orcamento;
import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID> {
    List<Orcamento> findByOrdemServicoId(UUID osId);
    Optional<Orcamento> findByOrdemServicoIdAndStatus(UUID osId, StatusOrcamento status);
    boolean existsByOrdemServicoIdAndStatus(UUID osId, StatusOrcamento status);
}
