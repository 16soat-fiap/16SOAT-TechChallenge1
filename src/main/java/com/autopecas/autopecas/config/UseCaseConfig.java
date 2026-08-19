package com.autopecas.autopecas.config;

import com.autopecas.autopecas.application.port.in.ConsultaDeIndicadores;
import com.autopecas.autopecas.application.port.in.GestaoDeClientes;
import com.autopecas.autopecas.application.port.in.GestaoDeFuncionarios;
import com.autopecas.autopecas.application.port.in.GestaoDeOrcamentos;
import com.autopecas.autopecas.application.port.in.GestaoDeOrdensServico;
import com.autopecas.autopecas.application.port.in.GestaoDePecas;
import com.autopecas.autopecas.application.port.in.GestaoDeServicos;
import com.autopecas.autopecas.application.port.in.GestaoDeVeiculos;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.application.port.out.ConsultaIndicadores;
import com.autopecas.autopecas.application.port.out.ConsultaOrdemServico;
import com.autopecas.autopecas.application.port.out.FuncionarioRepositorio;
import com.autopecas.autopecas.application.port.out.GeradorMatricula;
import com.autopecas.autopecas.application.port.out.GeradorNumeroOS;
import com.autopecas.autopecas.application.port.out.HistoricoStatusOSRepositorio;
import com.autopecas.autopecas.application.port.out.MovimentacaoEstoqueRepositorio;
import com.autopecas.autopecas.application.port.out.OrcamentoRepositorio;
import com.autopecas.autopecas.application.port.out.OrdemServicoRepositorio;
import com.autopecas.autopecas.application.port.out.PecaRepositorio;
import com.autopecas.autopecas.application.port.out.Relogio;
import com.autopecas.autopecas.application.port.out.ServicoRepositorio;
import com.autopecas.autopecas.application.port.out.Transacao;
import com.autopecas.autopecas.application.port.out.VeiculoRepositorio;
import com.autopecas.autopecas.application.usecase.ConsultaDeIndicadoresUseCase;
import com.autopecas.autopecas.application.usecase.GestaoDeClientesUseCase;
import com.autopecas.autopecas.application.usecase.GestaoDeFuncionariosUseCase;
import com.autopecas.autopecas.application.usecase.GestaoDeOrcamentosUseCase;
import com.autopecas.autopecas.application.usecase.GestaoDeOrdensServicoUseCase;
import com.autopecas.autopecas.application.usecase.GestaoDePecasUseCase;
import com.autopecas.autopecas.application.usecase.GestaoDeServicosUseCase;
import com.autopecas.autopecas.application.usecase.GestaoDeVeiculosUseCase;
import com.autopecas.autopecas.domain.service.MovimentadorDeEstoque;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring dos casos de uso e do domain service.
 *
 * <p>É aqui que o Spring encontra o hexágono. As classes de domínio e de aplicação não têm
 * nenhuma anotação de framework — quem as transforma em beans é esta configuração, que fica na
 * borda. Trocar o Spring por outro container significaria reescrever apenas este arquivo.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public MovimentadorDeEstoque movimentadorDeEstoque() {
        return new MovimentadorDeEstoque();
    }

    @Bean
    public GestaoDeClientes gestaoDeClientes(ClienteRepositorio clienteRepositorio,
                                            Transacao transacao) {
        return new GestaoDeClientesUseCase(clienteRepositorio, transacao);
    }

    @Bean
    public GestaoDeVeiculos gestaoDeVeiculos(VeiculoRepositorio veiculoRepositorio,
                                             ClienteRepositorio clienteRepositorio,
                                             Transacao transacao) {
        return new GestaoDeVeiculosUseCase(veiculoRepositorio, clienteRepositorio, transacao);
    }

    @Bean
    public GestaoDeFuncionarios gestaoDeFuncionarios(FuncionarioRepositorio funcionarioRepositorio,
                                                     GeradorMatricula geradorMatricula,
                                                     Transacao transacao) {
        return new GestaoDeFuncionariosUseCase(funcionarioRepositorio, geradorMatricula, transacao);
    }

    @Bean
    public GestaoDeServicos gestaoDeServicos(ServicoRepositorio servicoRepositorio,
                                             Transacao transacao) {
        return new GestaoDeServicosUseCase(servicoRepositorio, transacao);
    }

    @Bean
    public GestaoDePecas gestaoDePecas(PecaRepositorio pecaRepositorio,
                                       MovimentacaoEstoqueRepositorio movimentacaoRepositorio,
                                       MovimentadorDeEstoque movimentadorDeEstoque,
                                       Transacao transacao) {
        return new GestaoDePecasUseCase(pecaRepositorio, movimentacaoRepositorio,
                movimentadorDeEstoque, transacao);
    }

    @Bean
    public GestaoDeOrdensServico gestaoDeOrdensServico(
            OrdemServicoRepositorio ordemServicoRepositorio,
            ConsultaOrdemServico consultaOrdemServico,
            ClienteRepositorio clienteRepositorio,
            VeiculoRepositorio veiculoRepositorio,
            FuncionarioRepositorio funcionarioRepositorio,
            HistoricoStatusOSRepositorio historicoRepositorio,
            GeradorNumeroOS geradorNumeroOS,
            Relogio relogio,
            Transacao transacao) {
        return new GestaoDeOrdensServicoUseCase(ordemServicoRepositorio, consultaOrdemServico,
                clienteRepositorio, veiculoRepositorio, funcionarioRepositorio, historicoRepositorio,
                geradorNumeroOS, relogio, transacao);
    }

    @Bean
    public GestaoDeOrcamentos gestaoDeOrcamentos(
            OrcamentoRepositorio orcamentoRepositorio,
            OrdemServicoRepositorio ordemServicoRepositorio,
            ServicoRepositorio servicoRepositorio,
            PecaRepositorio pecaRepositorio,
            MovimentacaoEstoqueRepositorio movimentacaoRepositorio,
            MovimentadorDeEstoque movimentadorDeEstoque,
            Relogio relogio,
            Transacao transacao) {
        return new GestaoDeOrcamentosUseCase(orcamentoRepositorio, ordemServicoRepositorio,
                servicoRepositorio, pecaRepositorio, movimentacaoRepositorio, movimentadorDeEstoque,
                relogio, transacao);
    }

    @Bean
    public ConsultaDeIndicadores consultaDeIndicadores(ConsultaIndicadores consultaIndicadores) {
        return new ConsultaDeIndicadoresUseCase(consultaIndicadores);
    }
}
