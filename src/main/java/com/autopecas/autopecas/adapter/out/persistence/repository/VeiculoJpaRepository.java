package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.VeiculoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repositório Spring Data de veículos. */
public interface VeiculoJpaRepository extends JpaRepository<VeiculoJpaEntity, UUID> {

    List<VeiculoJpaEntity> findAllByAtivoTrue();

    Optional<VeiculoJpaEntity> findByIdAndAtivoTrue(UUID id);

    Optional<VeiculoJpaEntity> findByPlacaAndAtivoTrue(String placa);

    Optional<VeiculoJpaEntity> findByChassi(String chassi);

    Optional<VeiculoJpaEntity> findByRenavam(String renavam);

    List<VeiculoJpaEntity> findByClienteIdAndAtivoTrue(UUID clienteId);
}
