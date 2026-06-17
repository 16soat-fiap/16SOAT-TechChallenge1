package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.enums.StatusOS;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID> {

    Optional<OrdemServico> findByNumero(String numero);
    Page<OrdemServico> findByStatus(StatusOS status, Pageable pageable);
    Page<OrdemServico> findByClienteId(UUID clienteId, Pageable pageable);
    Page<OrdemServico> findByMecanicoResponsavelId(UUID mecanicoId, Pageable pageable);

    @Query("SELECT o FROM OrdemServico o WHERE o.createdAt BETWEEN :inicio AND :fim")
    Page<OrdemServico> findByPeriodo(@Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim,
                                     Pageable pageable);

    @Query(value = "SELECT nextval('os_numero_seq')", nativeQuery = true)
    Long proximoNumero();

    List<OrdemServico> findByStatusIn(List<StatusOS> statusOS);

    long countByStatus(StatusOS status);
}
