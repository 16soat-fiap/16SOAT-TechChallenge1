package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.ClientePF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientePFRepository extends JpaRepository<ClientePF, UUID> {
    Optional<ClientePF> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
}
