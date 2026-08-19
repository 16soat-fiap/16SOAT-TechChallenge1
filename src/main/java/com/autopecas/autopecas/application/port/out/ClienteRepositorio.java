package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.domain.model.cliente.Cliente;
import com.autopecas.autopecas.domain.model.cliente.ClientePF;
import com.autopecas.autopecas.domain.model.cliente.ClientePJ;
import com.autopecas.autopecas.domain.vo.CNPJ;
import com.autopecas.autopecas.domain.vo.CPF;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída do agregado Cliente.
 *
 * <p>Concentra em uma única port o que antes eram três interfaces Spring Data
 * (Cliente, ClientePF e ClientePJ) — a polimorfia PF/PJ agora vive no domínio.
 */
public interface ClienteRepositorio {

    /** Persiste e devolve o agregado já com o id atribuído pelo banco. */
    Cliente salvar(Cliente cliente);

    Optional<Cliente> porId(UUID id);

    boolean existePorId(UUID id);

    List<Cliente> ativos();

    Optional<ClientePF> porCpf(CPF cpf);

    Optional<ClientePJ> porCnpj(CNPJ cnpj);

    boolean existePorCpf(CPF cpf);

    boolean existePorCnpj(CNPJ cnpj);
}
