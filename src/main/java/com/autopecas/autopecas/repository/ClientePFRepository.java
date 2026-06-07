package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.ClientePF;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientePFRepository extends JpaRepository<ClientePF, UUID> {
    Optional<ClientePF> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
}
