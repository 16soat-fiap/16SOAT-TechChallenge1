package com.autopecas.autopecas.domain.model.os;

import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Raiz do agregado Ordem de Serviço.
 *
 * <p>Fluxo de status:
 * RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE.
 * A partir de AGUARDANDO_APROVACAO também é possível voltar para EM_DIAGNOSTICO, quando o
 * cliente rejeita o orçamento e uma nova versão precisa ser elaborada.
 *
 * <p>O agregado contém os itens efetivamente executados (serviços e peças). Orçamento e
 * histórico de status são agregados próprios, referenciados por id — a versão vigente e o
 * orçamento aprovado são consultados pelo respectivo repositório, não por esta classe.
 *
 * <p>valorTotalAprovado é uma cópia do total do orçamento aprovado, mantida para leitura rápida.
 */
public final class OrdemServico {

    private static final Map<StatusOS, Set<StatusOS>> TRANSICOES_PERMITIDAS = Map.of(
            StatusOS.RECEBIDA,             Set.of(StatusOS.EM_DIAGNOSTICO),
            StatusOS.EM_DIAGNOSTICO,       Set.of(StatusOS.AGUARDANDO_APROVACAO),
            StatusOS.AGUARDANDO_APROVACAO, Set.of(StatusOS.EM_EXECUCAO, StatusOS.EM_DIAGNOSTICO),
            StatusOS.EM_EXECUCAO,          Set.of(StatusOS.FINALIZADA),
            StatusOS.FINALIZADA,           Set.of(StatusOS.ENTREGUE),
            StatusOS.ENTREGUE,             Set.of()
    );

    private final UUID id;
    private final String numero;
    /** Espelha a coluna de lock otimista. Nulo enquanto a OS não foi persistida. */
    private final Long versao;
    private StatusOS status;
    private final Integer quilometragemEntrada;
    private final String observacoesEntrada;
    private String diagnostico;
    private final String queixaCliente;
    private BigDecimal valorTotalAprovado;
    private LocalDateTime dataInicioExecucao;
    private LocalDateTime dataFinalizacao;
    private LocalDateTime dataEntrega;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;
    private final UUID clienteId;
    private final UUID veiculoId;
    private UUID atendenteRecepcaoId;
    private UUID atendenteEntregaId;
    private UUID mecanicoResponsavelId;
    private final List<ItemServicoOS> itensServico;
    private final List<ItemPecaOS> itensPeca;

    private OrdemServico(UUID id, String numero, Long versao, StatusOS status,
                         Integer quilometragemEntrada, String observacoesEntrada, String diagnostico,
                         String queixaCliente, BigDecimal valorTotalAprovado,
                         LocalDateTime dataInicioExecucao, LocalDateTime dataFinalizacao,
                         LocalDateTime dataEntrega, LocalDateTime criadoEm, LocalDateTime atualizadoEm,
                         UUID clienteId, UUID veiculoId, UUID atendenteRecepcaoId, UUID atendenteEntregaId,
                         UUID mecanicoResponsavelId, List<ItemServicoOS> itensServico,
                         List<ItemPecaOS> itensPeca) {
        if (numero == null || numero.isBlank()) {
            throw new BusinessException("Número da OS é obrigatório");
        }
        if (clienteId == null) {
            throw new BusinessException("OS deve referenciar um cliente");
        }
        if (veiculoId == null) {
            throw new BusinessException("OS deve referenciar um veículo");
        }
        this.id = id;
        this.numero = numero;
        this.versao = versao;
        this.status = status;
        this.quilometragemEntrada = quilometragemEntrada;
        this.observacoesEntrada = observacoesEntrada;
        this.diagnostico = diagnostico;
        this.queixaCliente = queixaCliente;
        this.valorTotalAprovado = valorTotalAprovado == null ? BigDecimal.ZERO : valorTotalAprovado;
        this.dataInicioExecucao = dataInicioExecucao;
        this.dataFinalizacao = dataFinalizacao;
        this.dataEntrega = dataEntrega;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.atendenteRecepcaoId = atendenteRecepcaoId;
        this.atendenteEntregaId = atendenteEntregaId;
        this.mecanicoResponsavelId = mecanicoResponsavelId;
        this.itensServico = itensServico == null ? new ArrayList<>() : new ArrayList<>(itensServico);
        this.itensPeca = itensPeca == null ? new ArrayList<>() : new ArrayList<>(itensPeca);
    }

    /**
     * Abre uma nova OS em status RECEBIDA. O número vem do gerador de numeração e o
     * atendente de recepção é opcional (nulo quando a origem não é um atendente identificado).
     */
    public static OrdemServico abrir(String numero, UUID clienteId, UUID veiculoId, String queixaCliente,
                                     String observacoesEntrada, Integer quilometragemEntrada,
                                     UUID atendenteRecepcaoId) {
        return new OrdemServico(null, numero, null, StatusOS.RECEBIDA, quilometragemEntrada,
                observacoesEntrada, null, queixaCliente, BigDecimal.ZERO, null, null, null, null, null,
                clienteId, veiculoId, atendenteRecepcaoId, null, null, new ArrayList<>(), new ArrayList<>());
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static OrdemServico reconstituir(UUID id, String numero, Long versao, StatusOS status,
                                            Integer quilometragemEntrada, String observacoesEntrada,
                                            String diagnostico, String queixaCliente,
                                            BigDecimal valorTotalAprovado,
                                            LocalDateTime dataInicioExecucao,
                                            LocalDateTime dataFinalizacao, LocalDateTime dataEntrega,
                                            LocalDateTime criadoEm, LocalDateTime atualizadoEm,
                                            UUID clienteId, UUID veiculoId, UUID atendenteRecepcaoId,
                                            UUID atendenteEntregaId, UUID mecanicoResponsavelId,
                                            List<ItemServicoOS> itensServico, List<ItemPecaOS> itensPeca) {
        return new OrdemServico(id, numero, versao, status, quilometragemEntrada, observacoesEntrada,
                diagnostico, queixaCliente, valorTotalAprovado, dataInicioExecucao, dataFinalizacao,
                dataEntrega, criadoEm, atualizadoEm, clienteId, veiculoId, atendenteRecepcaoId,
                atendenteEntregaId, mecanicoResponsavelId, itensServico, itensPeca);
    }

    /**
     * Valida e aplica a transição de status, registrando a data do marco correspondente.
     *
     * @throws BusinessException se a transição não for permitida a partir do status atual
     */
    public void avancarStatus(StatusOS novoStatus, LocalDateTime agora) {
        Set<StatusOS> permitidos = TRANSICOES_PERMITIDAS.getOrDefault(this.status, Set.of());
        if (!permitidos.contains(novoStatus)) {
            throw new BusinessException(
                    String.format("Transição de status inválida: %s → %s. Permitidos a partir de %s: %s",
                            this.status, novoStatus, this.status, permitidos));
        }

        switch (novoStatus) {
            case EM_EXECUCAO -> this.dataInicioExecucao = agora;
            case FINALIZADA  -> this.dataFinalizacao = agora;
            case ENTREGUE    -> this.dataEntrega = agora;
            default -> { /* sem efeito colateral */ }
        }
        this.status = novoStatus;
    }

    /** O diagnóstico só pode ser registrado enquanto a OS está em diagnóstico. */
    public void registrarDiagnostico(String diagnostico) {
        if (this.status != StatusOS.EM_DIAGNOSTICO) {
            throw new BusinessException(
                    "Diagnóstico só pode ser registrado quando a OS está em EM_DIAGNOSTICO. Status atual: "
                            + this.status);
        }
        this.diagnostico = diagnostico;
    }

    /**
     * Define o mecânico responsável. A verificação de que o funcionário é de fato um mecânico
     * depende de consulta ao repositório e por isso acontece no caso de uso.
     */
    public void atribuirMecanico(UUID mecanicoId) {
        if (mecanicoId == null) {
            throw new BusinessException("Mecânico responsável é obrigatório");
        }
        this.mecanicoResponsavelId = mecanicoId;
    }

    public void registrarAtendenteEntrega(UUID atendenteId) {
        this.atendenteEntregaId = atendenteId;
    }

    /**
     * Efeito da aprovação de um orçamento sobre a OS: copia o valor aprovado e avança
     * para EM_EXECUCAO.
     */
    public void registrarAprovacaoDeOrcamento(BigDecimal valorTotalAprovado, LocalDateTime agora) {
        avancarStatus(StatusOS.EM_EXECUCAO, agora);
        this.valorTotalAprovado = valorTotalAprovado;
    }

    public void adicionarItemServico(ItemServicoOS item) {
        this.itensServico.add(item);
    }

    public void adicionarItemPeca(ItemPecaOS item) {
        this.itensPeca.add(item);
    }

    /** Tempo total de execução em minutos, ou nulo se a OS ainda não foi finalizada. */
    public Long calcularTempoExecucaoMinutos() {
        return calcularTempoExecucaoMinutos(dataInicioExecucao, dataFinalizacao);
    }

    /**
     * Mesma regra de cálculo do tempo de execução, aplicável a projeções de leitura que
     * carregam apenas as duas datas — evita duplicar a regra fora do domínio.
     */
    public static Long calcularTempoExecucaoMinutos(LocalDateTime inicioExecucao,
                                                    LocalDateTime finalizacao) {
        if (inicioExecucao == null || finalizacao == null) {
            return null;
        }
        return Duration.between(inicioExecucao, finalizacao).toMinutes();
    }

    /** Soma dos subtotais de serviços e peças efetivamente lançados na OS. */
    public BigDecimal calcularTotalExecutado() {
        BigDecimal servicos = itensServico.stream()
                .map(ItemServicoOS::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pecas = itensPeca.stream()
                .map(ItemPecaOS::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return servicos.add(pecas);
    }

    public boolean isNovo() {
        return id == null;
    }

    public UUID getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public Long getVersao() {
        return versao;
    }

    public StatusOS getStatus() {
        return status;
    }

    public Integer getQuilometragemEntrada() {
        return quilometragemEntrada;
    }

    public String getObservacoesEntrada() {
        return observacoesEntrada;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public String getQueixaCliente() {
        return queixaCliente;
    }

    public BigDecimal getValorTotalAprovado() {
        return valorTotalAprovado;
    }

    public LocalDateTime getDataInicioExecucao() {
        return dataInicioExecucao;
    }

    public LocalDateTime getDataFinalizacao() {
        return dataFinalizacao;
    }

    public LocalDateTime getDataEntrega() {
        return dataEntrega;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getVeiculoId() {
        return veiculoId;
    }

    public UUID getAtendenteRecepcaoId() {
        return atendenteRecepcaoId;
    }

    public UUID getAtendenteEntregaId() {
        return atendenteEntregaId;
    }

    public UUID getMecanicoResponsavelId() {
        return mecanicoResponsavelId;
    }

    /** Itens de serviço em lista imutável — use adicionarItemServico para incluir. */
    public List<ItemServicoOS> getItensServico() {
        return List.copyOf(itensServico);
    }

    /** Itens de peça em lista imutável — use adicionarItemPeca para incluir. */
    public List<ItemPecaOS> getItensPeca() {
        return List.copyOf(itensPeca);
    }

    /** Ordenação estável dos itens de serviço por id, para saídas determinísticas. */
    public List<ItemServicoOS> getItensServicoOrdenados() {
        return itensServico.stream()
                .sorted(Comparator.comparing(ItemServicoOS::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
