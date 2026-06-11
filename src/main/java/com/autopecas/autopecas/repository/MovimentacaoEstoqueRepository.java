package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID> {
    List<MovimentacaoEstoque> findByPecaIdOrderByCreatedAtDesc(UUID pecaId);
    List<MovimentacaoEstoque> findByOrdemServicoId(UUID osId);
    List<MovimentacaoEstoque> findByPecaId(UUID pecaId);
}
