package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.ClientePJJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Repositório Spring Data do cliente PJ, para as consultas por CNPJ. */
public interface ClientePJJpaRepository extends JpaRepository<ClientePJJpaEntity, UUID> {

    Optional<ClientePJJpaEntity> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}
