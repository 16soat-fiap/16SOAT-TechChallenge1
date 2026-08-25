package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.port.in.ControleDeAcessoDoCliente;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.application.port.out.OrdemServicoRepositorio;
import com.autopecas.autopecas.application.port.out.VeiculoRepositorio;
import com.autopecas.autopecas.domain.model.cliente.Cliente;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolve a propriedade de um recurso a partir do e-mail do usuário autenticado.
 *
 * <p>Todas as respostas são booleanas e nunca lançam: um recurso inexistente responde "não é
 * seu", e quem decide se isso vira 403 ou 404 é a camada de autorização. Isso também evita que
 * a checagem sirva de oráculo para descobrir quais ids existem.
 */
public class ControleDeAcessoDoClienteUseCase implements ControleDeAcessoDoCliente {

    private final ClienteRepositorio clienteRepositorio;
    private final VeiculoRepositorio veiculoRepositorio;
    private final OrdemServicoRepositorio ordemServicoRepositorio;

    public ControleDeAcessoDoClienteUseCase(ClienteRepositorio clienteRepositorio,
                                            VeiculoRepositorio veiculoRepositorio,
                                            OrdemServicoRepositorio ordemServicoRepositorio) {
        this.clienteRepositorio = clienteRepositorio;
        this.veiculoRepositorio = veiculoRepositorio;
        this.ordemServicoRepositorio = ordemServicoRepositorio;
    }

    @Override
    public boolean ehOProprioCliente(String email, UUID clienteId) {
        if (clienteId == null) {
            return false;
        }
        return idDoClienteAutenticado(email).filter(clienteId::equals).isPresent();
    }

    @Override
    public boolean ehDonoDoVeiculo(String email, UUID veiculoId) {
        if (veiculoId == null) {
            return false;
        }
        return idDoClienteAutenticado(email)
                .flatMap(clienteId -> veiculoRepositorio.porId(veiculoId)
                        .map(veiculo -> clienteId.equals(veiculo.getClienteId())))
                .orElse(false);
    }

    @Override
    public boolean ehDonoDaOrdemServico(String email, UUID ordemServicoId) {
        if (ordemServicoId == null) {
            return false;
        }
        return idDoClienteAutenticado(email)
                .flatMap(clienteId -> ordemServicoRepositorio.porId(ordemServicoId)
                        .map(os -> clienteId.equals(os.getClienteId())))
                .orElse(false);
    }

    @Override
    public boolean ehDonoDaOrdemServicoPorNumero(String email, String numero) {
        if (numero == null || numero.isBlank()) {
            return false;
        }
        return idDoClienteAutenticado(email)
                .flatMap(clienteId -> ordemServicoRepositorio.porNumero(numero)
                        .map(os -> clienteId.equals(os.getClienteId())))
                .orElse(false);
    }

    private Optional<UUID> idDoClienteAutenticado(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return clienteRepositorio.porEmail(email).map(Cliente::getId);
    }
}
