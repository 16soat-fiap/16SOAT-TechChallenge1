package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.pagination.Pagina;
import com.autopecas.autopecas.application.pagination.PaginaRequisicao;
import com.autopecas.autopecas.application.port.in.GestaoDeOrdensServico;
import com.autopecas.autopecas.application.port.in.view.OrdemServicoView;
import com.autopecas.autopecas.application.port.out.ClienteRepositorio;
import com.autopecas.autopecas.application.port.out.ConsultaOrdemServico;
import com.autopecas.autopecas.application.port.out.FuncionarioRepositorio;
import com.autopecas.autopecas.application.port.out.GeradorNumeroOS;
import com.autopecas.autopecas.application.port.out.HistoricoStatusOSRepositorio;
import com.autopecas.autopecas.application.port.out.OrdemServicoRepositorio;
import com.autopecas.autopecas.application.port.out.Relogio;
import com.autopecas.autopecas.application.port.out.Transacao;
import com.autopecas.autopecas.application.port.out.VeiculoRepositorio;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.funcionario.Atendente;
import com.autopecas.autopecas.domain.model.funcionario.Funcionario;
import com.autopecas.autopecas.domain.model.funcionario.Mecanico;
import com.autopecas.autopecas.domain.model.os.HistoricoStatusOS;
import com.autopecas.autopecas.domain.model.os.OrdemServico;

import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso do agregado OrdemServico.
 *
 * <p>As leituras usam a query port ConsultaOrdemServico, que já devolve o nome do cliente e a
 * placa do veículo por projeção. As escritas carregam o agregado, aplicam a regra de domínio,
 * salvam e registram o histórico da transição.
 */
public class GestaoDeOrdensServicoUseCase implements GestaoDeOrdensServico {

    private final OrdemServicoRepositorio ordemServicoRepositorio;
    private final ConsultaOrdemServico consultaOrdemServico;
    private final ClienteRepositorio clienteRepositorio;
    private final VeiculoRepositorio veiculoRepositorio;
    private final FuncionarioRepositorio funcionarioRepositorio;
    private final HistoricoStatusOSRepositorio historicoRepositorio;
    private final GeradorNumeroOS geradorNumeroOS;
    private final Relogio relogio;
    private final Transacao transacao;

    public GestaoDeOrdensServicoUseCase(OrdemServicoRepositorio ordemServicoRepositorio,
                                        ConsultaOrdemServico consultaOrdemServico,
                                        ClienteRepositorio clienteRepositorio,
                                        VeiculoRepositorio veiculoRepositorio,
                                        FuncionarioRepositorio funcionarioRepositorio,
                                        HistoricoStatusOSRepositorio historicoRepositorio,
                                        GeradorNumeroOS geradorNumeroOS,
                                        Relogio relogio,
                                        Transacao transacao) {
        this.ordemServicoRepositorio = ordemServicoRepositorio;
        this.consultaOrdemServico = consultaOrdemServico;
        this.clienteRepositorio = clienteRepositorio;
        this.veiculoRepositorio = veiculoRepositorio;
        this.funcionarioRepositorio = funcionarioRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.geradorNumeroOS = geradorNumeroOS;
        this.relogio = relogio;
        this.transacao = transacao;
    }

    @Override
    public Pagina<OrdemServicoView> listar(StatusOS status, UUID clienteId, UUID mecanicoId,
                                           PaginaRequisicao paginacao) {
        return consultaOrdemServico.listar(
                new ConsultaOrdemServico.Filtro(status, clienteId, mecanicoId), paginacao);
    }

    @Override
    public OrdemServicoView porNumero(String numero) {
        return consultaOrdemServico.porNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ordem de serviço não encontrada com número: " + numero));
    }

    @Override
    public OrdemServicoView abrir(Abrir comando) {
        return transacao.executar(() -> {
            if (!clienteRepositorio.existePorId(comando.clienteId())) {
                throw new ResourceNotFoundException(
                        "Cliente não encontrado com ID: " + comando.clienteId());
            }
            veiculoRepositorio.porIdAtivo(comando.veiculoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Veículo inativo ou não encontrado com ID: " + comando.veiculoId()));

            Atendente atendente = atendentePorEmail(comando.emailAtendente()).orElse(null);
            UUID atendenteId = atendente != null ? atendente.getId() : null;

            OrdemServico os = OrdemServico.abrir(geradorNumeroOS.proximo(), comando.clienteId(),
                    comando.veiculoId(), comando.queixaCliente(), comando.observacoesEntrada(),
                    comando.quilometragemEntrada(), atendenteId);

            OrdemServico salva = ordemServicoRepositorio.salvar(os);

            HistoricoStatusOS historico = atendente != null
                    ? HistoricoStatusOS.abertura(salva.getId(), atendente)
                    : HistoricoStatusOS.porSistema(salva.getId(), null, StatusOS.RECEBIDA,
                            "Ordem de serviço aberta.");
            historicoRepositorio.salvar(historico);

            return recarregarView(salva.getId());
        });
    }

    @Override
    public OrdemServicoView avancarStatus(UUID id, AvancarStatus comando) {
        StatusOS novoStatus = converterStatus(comando.novoStatus());

        return transacao.executar(() -> {
            OrdemServico os = buscar(id);
            StatusOS statusAnterior = os.getStatus();

            os.avancarStatus(novoStatus, relogio.agora());
            OrdemServico atualizada = ordemServicoRepositorio.salvar(os);

            Funcionario funcionario = comando.emailFuncionario() != null
                    ? funcionarioRepositorio.porEmail(comando.emailFuncionario()).orElse(null)
                    : null;

            HistoricoStatusOS historico = funcionario != null
                    ? HistoricoStatusOS.porFuncionario(atualizada.getId(), statusAnterior, novoStatus,
                            comando.observacao(), funcionario)
                    : HistoricoStatusOS.porSistema(atualizada.getId(), statusAnterior, novoStatus,
                            comando.observacao());
            historicoRepositorio.salvar(historico);

            return recarregarView(atualizada.getId());
        });
    }

    @Override
    public OrdemServicoView registrarDiagnostico(UUID id, String diagnostico) {
        return transacao.executar(() -> {
            OrdemServico os = buscar(id);
            os.registrarDiagnostico(diagnostico);
            OrdemServico salva = ordemServicoRepositorio.salvar(os);
            return recarregarView(salva.getId());
        });
    }

    @Override
    public OrdemServicoView atribuirMecanico(UUID id, UUID mecanicoId) {
        return transacao.executar(() -> {
            OrdemServico os = buscar(id);

            Funcionario funcionario = funcionarioRepositorio.porId(mecanicoId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Mecânico não encontrado com ID: " + mecanicoId));
            if (!(funcionario instanceof Mecanico)) {
                throw new BusinessException("O funcionário informado não é um mecânico.");
            }

            os.atribuirMecanico(mecanicoId);
            OrdemServico atualizada = ordemServicoRepositorio.salvar(os);
            return recarregarView(atualizada.getId());
        });
    }

    private OrdemServico buscar(UUID id) {
        return ordemServicoRepositorio.porId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ordem de serviço não encontrada com ID: " + id));
    }

    private Optional<Atendente> atendentePorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return funcionarioRepositorio.porEmail(email)
                .filter(Atendente.class::isInstance)
                .map(Atendente.class::cast);
    }

    private StatusOS converterStatus(String novoStatus) {
        try {
            return StatusOS.valueOf(novoStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status inválido: " + novoStatus);
        }
    }

    /** Relê a projeção para devolver a view já com nome do cliente e placa do veículo. */
    private OrdemServicoView recarregarView(UUID id) {
        return consultaOrdemServico.porId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ordem de serviço não encontrada com ID: " + id));
    }
}
