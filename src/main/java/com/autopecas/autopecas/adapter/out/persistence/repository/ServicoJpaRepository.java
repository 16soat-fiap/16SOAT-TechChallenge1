package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.ServicoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repositório Spring Data do catálogo de serviços. */
public interface ServicoJpaRepository extends JpaRepository<ServicoJpaEntity, UUID> {

    List<ServicoJpaEntity> findByAtivoTrue();
}
