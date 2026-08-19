package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.port.in.GestaoDeVeiculos;
import com.autopecas.autopecas.application.port.in.view.VeiculoView;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.application.port.out.Transacao;
import com.autopecas.autopecas.application.port.out.VeiculoRepositorio;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.veiculo.Veiculo;
import com.autopecas.autopecas.domain.vo.Placa;

import java.util.List;
import java.util.UUID;

/** Casos de uso do agregado Veiculo. */
public class GestaoDeVeiculosUseCase implements GestaoDeVeiculos {

    private final VeiculoRepositorio veiculoRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final Transacao transacao;

    public GestaoDeVeiculosUseCase(VeiculoRepositorio veiculoRepositorio,
                                   ClienteRepositorio clienteRepositorio,
                                   Transacao transacao) {
        this.veiculoRepositorio = veiculoRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.transacao = transacao;
    }

    @Override
    public List<VeiculoView> listarAtivos() {
        return veiculoRepositorio.ativos().stream().map(this::paraView).toList();
    }

    @Override
    public VeiculoView porId(UUID id) {
        return paraView(buscarAtivo(id, "Veiculo não encontrado, id " + id));
    }

    @Override
    public VeiculoView porPlaca(String placa) {
        Veiculo veiculo = veiculoRepositorio.porPlacaAtiva(new Placa(placa))
                .orElseThrow(() -> new ResourceNotFoundException("Placa " + placa + " Não encontrada"));
        return paraView(veiculo);
    }

    @Override
    public List<VeiculoView> doCliente(UUID clienteId) {
        if (!clienteRepositorio.existePorId(clienteId)) {
            throw new ResourceNotFoundException("Cliente não encontrado, id: " + clienteId);
        }
        return veiculoRepositorio.ativosDoCliente(clienteId).stream().map(this::paraView).toList();
    }

    @Override
    public VeiculoView cadastrar(Cadastrar comando) {
        Placa placa = new Placa(comando.placa());

        return transacao.executar(() -> {
            if (!clienteRepositorio.existePorId(comando.clienteId())) {
                throw new ResourceNotFoundException("Cliente não encontrado, id: " + comando.clienteId());
            }
            Veiculo veiculo = Veiculo.criar(placa, comando.chassi(), comando.renavam(), comando.marca(),
                    comando.modelo(), comando.anoModelo(), comando.cor(), comando.clienteId());
            return paraView(veiculoRepositorio.salvar(veiculo));
        });
    }

    @Override
    public VeiculoView atualizar(UUID id, AtualizarDados comando) {
        return transacao.executar(() -> {
            Veiculo veiculo = buscarAtivo(id, "Veículo não encontrado, id: " + id);

            if (comando.chassi() != null) {
                validarChassiUnico(comando.chassi(), id);
            }
            if (comando.renavam() != null) {
                validarRenavamUnico(comando.renavam(), id);
            }

            veiculo.atualizarDados(comando.marca(), comando.modelo(), comando.anoModelo(),
                    comando.cor(), comando.chassi(), comando.renavam());
            return paraView(veiculoRepositorio.salvar(veiculo));
        });
    }

    @Override
    public void desativar(UUID id) {
        transacao.executar(() -> {
            Veiculo veiculo = buscarAtivo(id, "Veículo não encontrado, id: " + id);
            veiculo.desativar();
            veiculoRepositorio.salvar(veiculo);
        });
    }

    private Veiculo buscarAtivo(UUID id, String mensagemSeAusente) {
        return veiculoRepositorio.porIdAtivo(id)
                .orElseThrow(() -> new ResourceNotFoundException(mensagemSeAusente));
    }

    private void validarChassiUnico(String chassi, UUID veiculoId) {
        veiculoRepositorio.porChassi(chassi)
                .filter(outro -> !outro.getId().equals(veiculoId))
                .ifPresent(outro -> {
                    throw new BusinessException("Chassi já cadastrado.");
                });
    }

    private void validarRenavamUnico(String renavam, UUID veiculoId) {
        veiculoRepositorio.porRenavam(renavam)
                .filter(outro -> !outro.getId().equals(veiculoId))
                .ifPresent(outro -> {
                    throw new BusinessException("RENAVAM já cadastrado.");
                });
    }

    private VeiculoView paraView(Veiculo veiculo) {
        return new VeiculoView(veiculo.getId(), veiculo.getPlaca().valor(), veiculo.getMarca(),
                veiculo.getModelo(), veiculo.getAnoModelo(), veiculo.getCor(), veiculo.isAtivo(),
                veiculo.getClienteId());
    }
}
