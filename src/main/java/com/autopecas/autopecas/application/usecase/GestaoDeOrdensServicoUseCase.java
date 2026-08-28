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
import com.autopecas.autopecas.application.port.out.NotificadorDeStatusOS;
import com.autopecas.autopecas.application.port.out.OrdemServicoRepositorio;
import com.autopecas.autopecas.application.port.out.PecaRepositorio;
import com.autopecas.autopecas.application.port.out.Relogio;
import com.autopecas.autopecas.application.port.out.ServicoRepositorio;
import com.autopecas.autopecas.application.port.out.Transacao;
import com.autopecas.autopecas.application.port.out.VeiculoRepositorio;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.cliente.Cliente;
import com.autopecas.autopecas.domain.model.estoque.Peca;
import com.autopecas.autopecas.domain.model.estoque.Servico;
import com.autopecas.autopecas.domain.model.funcionario.Atendente;
import com.autopecas.autopecas.domain.model.funcionario.Funcionario;
import com.autopecas.autopecas.domain.model.funcionario.Mecanico;
import com.autopecas.autopecas.domain.model.os.HistoricoStatusOS;
import com.autopecas.autopecas.domain.model.os.ItemPecaOS;
import com.autopecas.autopecas.domain.model.os.ItemServicoOS;
import com.autopecas.autopecas.domain.model.os.OrdemServico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Casos de uso do agregado OrdemServico.
 *
 * <p>As leituras usam a query port ConsultaOrdemServico, que já devolve o nome do cliente e a
 * placa do veículo por projeção. As escritas carregam o agregado, aplicam a regra de domínio,
 * salvam e registram o histórico da transição.
 */
public class GestaoDeOrdensServicoUseCase implements GestaoDeOrdensServico {

    /** Mesma convenção do orçamento: quantidade omitida vale 1. */
    private static final int QUANTIDADE_PADRAO = 1;

    private final OrdemServicoRepositorio ordemServicoRepositorio;
    private final ConsultaOrdemServico consultaOrdemServico;
    private final ClienteRepositorio clienteRepositorio;
    private final VeiculoRepositorio veiculoRepositorio;
    private final FuncionarioRepositorio funcionarioRepositorio;
    private final HistoricoStatusOSRepositorio historicoRepositorio;
    private final ServicoRepositorio servicoRepositorio;
    private final PecaRepositorio pecaRepositorio;
    private final NotificadorDeStatusOS notificador;
    private final GeradorNumeroOS geradorNumeroOS;
    private final Relogio relogio;
    private final Transacao transacao;

    public GestaoDeOrdensServicoUseCase(OrdemServicoRepositorio ordemServicoRepositorio,
                                        ConsultaOrdemServico consultaOrdemServico,
                                        ClienteRepositorio clienteRepositorio,
                                        VeiculoRepositorio veiculoRepositorio,
                                        FuncionarioRepositorio funcionarioRepositorio,
                                        HistoricoStatusOSRepositorio historicoRepositorio,
                                        ServicoRepositorio servicoRepositorio,
                                        PecaRepositorio pecaRepositorio,
                                        NotificadorDeStatusOS notificador,
                                        GeradorNumeroOS geradorNumeroOS,
                                        Relogio relogio,
                                        Transacao transacao) {
        this.servicoRepositorio = servicoRepositorio;
        this.pecaRepositorio = pecaRepositorio;
        this.notificador = notificador;
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

            adicionarItensDeServico(os, comando.itensServico());
            adicionarItensDePeca(os, comando.itensPeca());

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

        // Guarda o status de origem para compor a notificação, sem precisar de um tipo novo
        // neste pacote — onde toda classe é, por regra de arquitetura, um caso de uso.
        AtomicReference<StatusOS> statusDeOrigem = new AtomicReference<>();

        OrdemServicoView viewAtualizada = transacao.executar(() -> {
            OrdemServico os = buscar(id);
            StatusOS statusAnterior = os.getStatus();
            statusDeOrigem.set(statusAnterior);

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

        // Fora da transação de propósito: a notificação é consequência da mudança já confirmada.
        // Notificar dentro do commit faria uma indisponibilidade do servidor de e-mail reverter
        // um avanço de status que a oficina já executou no mundo real.
        notificarCliente(viewAtualizada, statusDeOrigem.get(), comando.observacao());
        return viewAtualizada;
    }

    /**
     * Avisa o cliente da mudança de status, quando ele aceita notificações e tem e-mail.
     *
     * <p>O contrato da port proíbe a implementação de lançar; o try/catch aqui é a segunda
     * barreira, cobrindo também a leitura do cliente — nenhuma delas pode transformar um avanço
     * de status bem-sucedido em erro para quem chamou a API.
     */
    private void notificarCliente(OrdemServicoView view, StatusOS statusAnterior, String observacao) {
        try {
            Cliente cliente = clienteRepositorio.porId(view.clienteId()).orElse(null);
            if (cliente == null || !cliente.isAceitaNotificacoes()
                    || cliente.getEmail() == null || cliente.getEmail().isBlank()) {
                return;
            }
            notificador.notificarMudancaDeStatus(new NotificadorDeStatusOS.MudancaDeStatus(
                    view.numero(),
                    statusAnterior != null ? statusAnterior.name() : null,
                    view.status(), cliente.getNome(), cliente.getEmail(), observacao));
        } catch (RuntimeException e) {
            // Silenciar é intencional: ver javadoc de NotificadorDeStatusOS.
        }
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

    /**
     * Lança na OS os serviços já acordados na recepção, ao preço vigente no catálogo.
     *
     * <p>O preço é copiado, não referenciado: uma alteração futura no catálogo não pode mudar
     * retroativamente o que foi combinado com o cliente.
     */
    private void adicionarItensDeServico(OrdemServico os, List<Abrir.ItemServico> itens) {
        if (itens == null) {
            return;
        }
        for (Abrir.ItemServico item : itens) {
            Servico servico = servicoRepositorio.porId(item.servicoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Serviço não encontrado com ID: " + item.servicoId()));
            os.adicionarItemServico(ItemServicoOS.criar(servico.getId(),
                    quantidadeOuPadrao(item.quantidade()), servico.getPrecoBase()));
        }
    }

    /**
     * Lança na OS as peças previstas na recepção. Não há baixa de estoque aqui — ver o javadoc
     * de {@link Abrir}.
     */
    private void adicionarItensDePeca(OrdemServico os, List<Abrir.ItemPeca> itens) {
        if (itens == null) {
            return;
        }
        for (Abrir.ItemPeca item : itens) {
            Peca peca = pecaRepositorio.porId(item.pecaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Peça não encontrada com ID: " + item.pecaId()));
            os.adicionarItemPeca(ItemPecaOS.criar(peca.getId(),
                    quantidadeOuPadrao(item.quantidade()), peca.getPrecoVenda()));
        }
    }

    private int quantidadeOuPadrao(Integer quantidade) {
        return quantidade != null ? quantidade : QUANTIDADE_PADRAO;
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
