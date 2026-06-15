package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {
    List<Veiculo> findAllByAtivoTrue();
    Optional<Veiculo> findByIdAndAtivoTrue(UUID id);
    Optional<Veiculo> findByPlacaAndAtivoTrue(String placa);
    List<Veiculo> findByClienteIdAndAtivoTrue(UUID clienteId);
    boolean existsByPlaca(String placa);
    boolean existsByChassi(String chassi);
}
