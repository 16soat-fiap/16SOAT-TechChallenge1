package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.HistoricoStatusOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoricoStatusOsRepository extends JpaRepository<HistoricoStatusOS, Long> {

    List<HistoricoStatusOS> findByOrdemServicoIdOrderByCreatedAtAsc(UUID osId);
}
