package com.autopecas.autopecas.repository;

import com.autopecas.autopecas.domain.entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {
    Optional<Funcionario> findByMatricula(String matricula);
    Optional<Funcionario> findByCpf(String cpf);
    List<Funcionario> findByAtivoTrue();
    boolean existsByCpf(String cpf);
    boolean existsByMatricula(String matricula);
}
