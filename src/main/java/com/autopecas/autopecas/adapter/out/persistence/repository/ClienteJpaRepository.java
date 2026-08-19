package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.ClienteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repositório Spring Data da hierarquia de clientes. */
public interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, UUID> {

    List<ClienteJpaEntity> findByAtivoTrue();
}
