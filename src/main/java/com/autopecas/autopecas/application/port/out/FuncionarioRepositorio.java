package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.domain.model.funcionario.Funcionario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de saída do agregado Funcionario (Mecanico e Atendente). */
public interface FuncionarioRepositorio {

    Funcionario salvar(Funcionario funcionario);

    Optional<Funcionario> porId(UUID id);

    Optional<Funcionario> porEmail(String email);

    List<Funcionario> ativos();

    boolean existePorCpf(String cpf);
}
