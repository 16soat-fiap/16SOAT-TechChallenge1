package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.port.in.GestaoDeServicos;
import com.autopecas.autopecas.application.port.in.view.ServicoView;
import com.autopecas.autopecas.application.port.out.ServicoRepositorio;
import com.autopecas.autopecas.application.port.out.Transacao;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.estoque.Servico;

import java.util.List;
import java.util.UUID;

/** Casos de uso do catálogo de serviços. */
public class GestaoDeServicosUseCase implements GestaoDeServicos {

    private final ServicoRepositorio servicoRepositorio;
    private final Transacao transacao;

    public GestaoDeServicosUseCase(ServicoRepositorio servicoRepositorio, Transacao transacao) {
        this.servicoRepositorio = servicoRepositorio;
        this.transacao = transacao;
    }

    @Override
    public List<ServicoView> listarAtivos() {
        return servicoRepositorio.ativos().stream().map(this::paraView).toList();
    }

    @Override
    public ServicoView porId(UUID id) {
        return paraView(buscar(id, "Servico " + id + " não encontrado"));
    }

    @Override
    public ServicoView cadastrar(DadosDoServico comando) {
        return transacao.executar(() -> {
            Servico servico = Servico.criar(comando.nome(), comando.descricao(), comando.precoBase(),
                    comando.tempoEstimadoMinutos());
            return paraView(servicoRepositorio.salvar(servico));
        });
    }

    @Override
    public ServicoView atualizar(UUID id, DadosDoServico comando) {
        return transacao.executar(() -> {
            Servico servico = buscar(id, "Serviço não encontrado, ID: " + id);
            servico.atualizarDados(comando.nome(), comando.descricao(), comando.precoBase(),
                    comando.tempoEstimadoMinutos());
            return paraView(servicoRepositorio.salvar(servico));
        });
    }

    @Override
    public void desativar(UUID id) {
        transacao.executar(() -> {
            Servico servico = buscar(id, "Serviço " + id + " não encontrado");
            servico.desativar();
            servicoRepositorio.salvar(servico);
        });
    }

    private Servico buscar(UUID id, String mensagemSeAusente) {
        return servicoRepositorio.porId(id)
                .orElseThrow(() -> new ResourceNotFoundException(mensagemSeAusente));
    }

    private ServicoView paraView(Servico servico) {
        return new ServicoView(servico.getId(), servico.getNome(), servico.getDescricao(),
                servico.getPrecoBase(), servico.getTempoEstimadoMinutos(), servico.isAtivo());
    }
}
