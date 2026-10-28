package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.port.in.ControleDeAcessoDoCliente;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.application.port.out.OrdemServicoRepositorio;
import com.autopecas.autopecas.application.port.out.VeiculoRepositorio;
import com.autopecas.autopecas.domain.model.cliente.Cliente;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolve a propriedade de um recurso a partir do UUID do usuário autenticado.
 *
 * <p>Todas as respostas são booleanas e nunca lançam: um recurso inexistente responde "não é seu",
 * e quem decide se isso vira 403 ou 404 é a camada de autorização. Isso também evita que a checagem
 * sirva de oráculo para descobrir quais ids existem.
 */
public class ControleDeAcessoDoClienteUseCase implements ControleDeAcessoDoCliente {

    private final ClienteRepositorio clienteRepositorio;
    private final VeiculoRepositorio veiculoRepositorio;
    private final OrdemServicoRepositorio ordemServicoRepositorio;

    public ControleDeAcessoDoClienteUseCase(
            ClienteRepositorio clienteRepositorio,
            VeiculoRepositorio veiculoRepositorio,
            OrdemServicoRepositorio ordemServicoRepositorio) {
        this.clienteRepositorio = clienteRepositorio;
        this.veiculoRepositorio = veiculoRepositorio;
        this.ordemServicoRepositorio = ordemServicoRepositorio;
    }

    @Override
    public boolean ehOProprioCliente(String subject, UUID clienteId) {
        if (clienteId == null) {
            return false;
        }
        return idDoClienteAutenticado(subject).filter(clienteId::equals).isPresent();
    }

    @Override
    public boolean ehDonoDoVeiculo(String subject, UUID veiculoId) {
        if (veiculoId == null) {
            return false;
        }
        return idDoClienteAutenticado(subject)
                .flatMap(
                        clienteId ->
                                veiculoRepositorio
                                        .porId(veiculoId)
                                        .map(veiculo -> clienteId.equals(veiculo.getClienteId())))
                .orElse(false);
    }

    @Override
    public boolean ehDonoDaOrdemServico(String subject, UUID ordemServicoId) {
        if (ordemServicoId == null) {
            return false;
        }
        return idDoClienteAutenticado(subject)
                .flatMap(
                        clienteId ->
                                ordemServicoRepositorio
                                        .porId(ordemServicoId)
                                        .map(os -> clienteId.equals(os.getClienteId())))
                .orElse(false);
    }

    @Override
    public boolean ehDonoDaOrdemServicoPorNumero(String subject, String numero) {
        if (numero == null || numero.isBlank()) {
            return false;
        }
        return idDoClienteAutenticado(subject)
                .flatMap(
                        clienteId ->
                                ordemServicoRepositorio
                                        .porNumero(numero)
                                        .map(os -> clienteId.equals(os.getClienteId())))
                .orElse(false);
    }

    private Optional<UUID> idDoClienteAutenticado(String subject) {
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }
        try {
            return clienteRepositorio
                    .porId(UUID.fromString(subject))
                    .filter(Cliente::isAtivo)
                    .map(Cliente::getId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
