package com.autopecas.autopecas.adapter.out.persistence.repository;

import com.autopecas.autopecas.adapter.out.persistence.entity.PecaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repositório Spring Data de peças. */
public interface PecaJpaRepository extends JpaRepository<PecaJpaEntity, UUID> {

    Optional<PecaJpaEntity> findByCodigo(String codigo);

    List<PecaJpaEntity> findByAtivoTrue();

    boolean existsByCodigo(String codigo);

    @Query("SELECT p FROM PecaJpaEntity p WHERE p.ativo = true "
            + "AND p.quantidadeEstoque <= p.quantidadeMinima")
    List<PecaJpaEntity> findEstoqueBaixo();

    @Query("SELECT COUNT(p) FROM PecaJpaEntity p WHERE p.ativo = true "
            + "AND p.quantidadeEstoque <= p.quantidadeMinima")
    long countEstoqueBaixo();
}
