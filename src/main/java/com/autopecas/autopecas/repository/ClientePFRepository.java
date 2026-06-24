package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.ClientePF;
import com.autopecas.autopecas.domain.valueobject.CPF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientePFRepository extends JpaRepository<ClientePF, UUID> {
    Optional<ClientePF> findByCpf(CPF cpf);
    boolean existsByCpf(CPF cpf);
}
