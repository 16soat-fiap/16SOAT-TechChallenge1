package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.ClientePJ;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientePJRepository extends JpaRepository<ClientePJ, UUID> {
    Optional<ClientePJ> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
}
