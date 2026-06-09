package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {
    Optional<Veiculo> findByPlaca(String placa);
    List<Veiculo> findByClienteId(UUID clienteId);
    boolean existsByPlaca(String placa);
    boolean existsByChassi(String chassi);
}
