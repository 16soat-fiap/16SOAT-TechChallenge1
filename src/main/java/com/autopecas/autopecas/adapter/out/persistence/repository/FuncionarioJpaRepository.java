package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.FuncionarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repositório Spring Data da hierarquia de funcionários. */
public interface FuncionarioJpaRepository extends JpaRepository<FuncionarioJpaEntity, UUID> {

    Optional<FuncionarioJpaEntity> findByEmail(String email);

    List<FuncionarioJpaEntity> findByAtivoTrue();

    boolean existsByCpf(String cpf);

    @Query(value = "SELECT nextval('mecanico_seq')", nativeQuery = true)
    Long proximoNumeroMecanico();

    @Query(value = "SELECT nextval('atendente_seq')", nativeQuery = true)
    Long proximoNumeroAtendente();
}
