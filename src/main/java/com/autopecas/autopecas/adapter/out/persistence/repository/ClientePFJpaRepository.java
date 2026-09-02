package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.ClientePFJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Repositório Spring Data do cliente PF, para as consultas por CPF. */
public interface ClientePFJpaRepository extends JpaRepository<ClientePFJpaEntity, UUID> {

    Optional<ClientePFJpaEntity> findByCpf(String cpf);

    boolean existsByCpf(String cpf);
}
