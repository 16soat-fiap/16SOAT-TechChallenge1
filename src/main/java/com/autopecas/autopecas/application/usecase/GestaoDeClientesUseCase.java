package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.port.in.GestaoDeClientes;
import com.autopecas.autopecas.application.port.in.view.ClienteView;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.application.port.out.Transacao;
import com.autopecas.autopecas.domain.enums.Genero;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.cliente.Cliente;
import com.autopecas.autopecas.domain.model.cliente.ClientePF;
import com.autopecas.autopecas.domain.model.cliente.ClientePJ;
import com.autopecas.autopecas.domain.vo.CNPJ;
import com.autopecas.autopecas.domain.vo.CPF;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso do agregado Cliente.
 *
 * <p>Sem anotação alguma de framework: as dependências entram pelo construtor e a fronteira
 * transacional é a port Transacao.
 */
public class GestaoDeClientesUseCase implements GestaoDeClientes {

    private static final int TAMANHO_CPF = 11;
    private static final int TAMANHO_CNPJ = 14;

    private final ClienteRepositorio clienteRepositorio;
    private final Transacao transacao;

    public GestaoDeClientesUseCase(ClienteRepositorio clienteRepositorio, Transacao transacao) {
        this.clienteRepositorio = clienteRepositorio;
        this.transacao = transacao;
    }

    @Override
    public List<ClienteView> listarAtivos() {
        return clienteRepositorio.ativos().stream().map(this::paraView).toList();
    }

    @Override
    public ClienteView porId(UUID id) {
        return paraView(buscar(id, "Cliente não encontrado através do ID: " + id));
    }

    @Override
    public ClienteView porDocumento(String documento) {
        Cliente cliente = switch (documento.length()) {
            case TAMANHO_CPF -> clienteRepositorio.porCpf(new CPF(documento))
                    .map(Cliente.class::cast)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
            case TAMANHO_CNPJ -> clienteRepositorio.porCnpj(new CNPJ(documento))
                    .map(Cliente.class::cast)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
            default -> throw new BusinessException(
                    "Documento inválido: esperado CPF (11 dígitos) ou CNPJ (14 dígitos)");
        };
        return paraView(cliente);
    }

    @Override
    public ClienteView cadastrarPF(CadastrarPF comando) {
        CPF cpf = new CPF(comando.cpf());
        Genero genero = converterGenero(comando.genero());

        return transacao.executar(() -> {
            if (clienteRepositorio.existePorCpf(cpf)) {
                throw new BusinessException("CPF já cadastrado: " + comando.cpf());
            }
            ClientePF cliente = ClientePF.criar(comando.nome(), comando.email(), comando.telefone(),
                    comando.aceitaNotificacoes(), cpf, comando.dataNascimento(), comando.rg(), genero);
            return paraView(clienteRepositorio.salvar(cliente));
        });
    }

    @Override
    public ClienteView cadastrarPJ(CadastrarPJ comando) {
        CNPJ cnpj = new CNPJ(comando.cnpj());

        return transacao.executar(() -> {
            if (clienteRepositorio.existePorCnpj(cnpj)) {
                throw new BusinessException("CNPJ já cadastrado: " + comando.cnpj());
            }
            ClientePJ cliente = ClientePJ.criar(comando.nome(), comando.email(), comando.telefone(),
                    comando.aceitaNotificacoes(), cnpj, comando.razaoSocial(), comando.inscricaoEstadual());
            return paraView(clienteRepositorio.salvar(cliente));
        });
    }

    @Override
    public ClienteView atualizar(UUID id, AtualizarDados comando) {
        return transacao.executar(() -> {
            Cliente cliente = buscar(id, "Cliente não encontrado, id: " + id);
            cliente.atualizarDados(comando.nome(), comando.email(), comando.telefone(),
                    comando.aceitaNotificacoes());
            return paraView(clienteRepositorio.salvar(cliente));
        });
    }

    @Override
    public void desativar(UUID id) {
        transacao.executar(() -> {
            Cliente cliente = buscar(id, "Cliente não encontrado com ID: " + id);
            cliente.desativar();
            clienteRepositorio.salvar(cliente);
        });
    }

    private Cliente buscar(UUID id, String mensagemSeAusente) {
        return clienteRepositorio.porId(id)
                .orElseThrow(() -> new ResourceNotFoundException(mensagemSeAusente));
    }

    private Genero converterGenero(String genero) {
        if (genero == null || genero.isBlank()) {
            return null;
        }
        try {
            return Genero.valueOf(genero.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Gênero inválido: " + genero);
        }
    }

    private ClienteView paraView(Cliente cliente) {
        return new ClienteView(cliente.getId(), cliente.getNome(), cliente.getDocumento(),
                cliente.getEmail(), cliente.getTelefone());
    }
}
