package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.entity.ClienteJpaEntity;
import com.autopecas.autopecas.adapter.out.persistence.mapper.ClienteJpaMapper;
import com.autopecas.autopecas.adapter.out.persistence.repository.ClienteJpaRepository;
import com.autopecas.autopecas.adapter.out.persistence.repository.ClientePFJpaRepository;
import com.autopecas.autopecas.adapter.out.persistence.repository.ClientePJJpaRepository;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.cliente.Cliente;
import com.autopecas.autopecas.domain.model.cliente.ClientePF;
import com.autopecas.autopecas.domain.model.cliente.ClientePJ;
import com.autopecas.autopecas.domain.vo.CNPJ;
import com.autopecas.autopecas.domain.vo.CPF;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de persistência do agregado Cliente.
 *
 * <p>Ao gravar um cliente existente, carrega a entidade gerenciada e aplica o estado do
 * agregado sobre ela, em vez de fazer merge de uma entidade destacada. Isso preserva as datas
 * de auditoria e o documento imutável, e evita que campos não carregados sejam anulados.
 */
@Component
public class ClientePersistenceAdapter implements ClienteRepositorio {

    private final ClienteJpaRepository clienteRepository;
    private final ClientePFJpaRepository clientePFRepository;
    private final ClientePJJpaRepository clientePJRepository;
    private final ClienteJpaMapper mapper;

    public ClientePersistenceAdapter(ClienteJpaRepository clienteRepository,
                                     ClientePFJpaRepository clientePFRepository,
                                     ClientePJJpaRepository clientePJRepository,
                                     ClienteJpaMapper mapper) {
        this.clienteRepository = clienteRepository;
        this.clientePFRepository = clientePFRepository;
        this.clientePJRepository = clientePJRepository;
        this.mapper = mapper;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        ClienteJpaEntity entidade;
        if (cliente.isNovo()) {
            entidade = mapper.novaEntidade(cliente);
        } else {
            entidade = clienteRepository.findById(cliente.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cliente não encontrado com ID: " + cliente.getId()));
            mapper.aplicar(cliente, entidade);
        }
        return mapper.paraDominio(clienteRepository.save(entidade));
    }

    @Override
    public Optional<Cliente> porId(UUID id) {
        return clienteRepository.findById(id).map(mapper::paraDominio);
    }

    @Override
    public Optional<Cliente> porEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return clienteRepository.findByEmailIgnoreCaseAndAtivoTrue(email).map(mapper::paraDominio);
    }

    @Override
    public boolean existePorId(UUID id) {
        return clienteRepository.existsById(id);
    }

    @Override
    public List<Cliente> ativos() {
        return clienteRepository.findByAtivoTrue().stream().map(mapper::paraDominio).toList();
    }

    @Override
    public Optional<ClientePF> porCpf(CPF cpf) {
        return clientePFRepository.findByCpf(cpf.valor())
                .map(mapper::paraDominio)
                .map(ClientePF.class::cast);
    }

    @Override
    public Optional<ClientePJ> porCnpj(CNPJ cnpj) {
        return clientePJRepository.findByCnpj(cnpj.valor())
                .map(mapper::paraDominio)
                .map(ClientePJ.class::cast);
    }

    @Override
    public boolean existePorCpf(CPF cpf) {
        return clientePFRepository.existsByCpf(cpf.valor());
    }

    @Override
    public boolean existePorCnpj(CNPJ cnpj) {
        return clientePJRepository.existsByCnpj(cnpj.valor());
    }
}
