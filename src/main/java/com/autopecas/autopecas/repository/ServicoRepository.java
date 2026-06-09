package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, UUID> {
    List<Servico> findByAtivoTrue();
    List<Servico> findByNomeContainingIgnoreCaseAndAtivoTrue(String nome);
}
