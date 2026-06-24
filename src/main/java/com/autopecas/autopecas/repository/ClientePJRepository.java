package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.ClientePJ;
import com.autopecas.autopecas.domain.valueobject.CNPJ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientePJRepository extends JpaRepository<ClientePJ, UUID> {
    Optional<ClientePJ> findByCnpj(CNPJ cnpj);
    boolean existsByCnpj(CNPJ cnpj);
}
