package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.Veiculo;
import com.autopecas.autopecas.domain.valueobject.Placa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {
    List<Veiculo> findAllByAtivoTrue();
    Optional<Veiculo> findByIdAndAtivoTrue(UUID id);
    Optional<Veiculo> findByPlaca(Placa placa);
    Optional<Veiculo> findByPlacaAndAtivoTrue(Placa placa);
    List<Veiculo> findByClienteIdAndAtivoTrue(UUID clienteId);
    boolean existsByPlaca(Placa placa);
    boolean existsByChassi(String chassi);
    boolean existsByRenavam(String renavam);
    Optional<Veiculo> findByChassi(String chassi);
    Optional<Veiculo> findByRenavam(String renavam);
}
