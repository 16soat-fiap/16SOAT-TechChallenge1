package com.autopecas.autopecas.domain.model.orcamento;

import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import com.autopecas.autopecas.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Raiz do agregado Orçamento — uma versão de proposta para uma Ordem de Serviço.
 *
 * <p>Modelagem:
 * <ul>
 *   <li>Uma OS pode ter múltiplos orçamentos, daí o versionamento.</li>
 *   <li>Cada orçamento é um snapshot dos itens (serviços e peças) no momento.</li>
 *   <li>Se o cliente rejeita, cria-se uma nova versão em vez de editar — preserva o
 *       histórico da negociação.</li>
 * </ul>
 *
 * <p>Ciclo de vida: RASCUNHO → ENVIADA → APROVADA | CANCELADA | EXPIRADO.
 *
 * <p>Apenas um orçamento por OS pode estar APROVADA — essa unicidade depende de consulta ao
 * repositório e é verificada no caso de uso.
 */
public final class Orcamento {

    private final UUID id;
    private final int versao;
    private StatusOrcamento status;
    /** Espelha a coluna de lock otimista. Nulo enquanto o orçamento não foi persistido. */
    private final Long versionLock;
    private BigDecimal valorMaoObra;
    private BigDecimal valorPecas;
    private BigDecimal valorAcrescimo;
    private BigDecimal valorTotal;
    private final String condicoesPagamento;
    private final Integer prazoExecucaoDias;
    private final LocalDate dataValidade;
    private final String observacoes;
    private LocalDateTime dataEnvio;
    private LocalDateTime dataRespostaCliente;
    private String motivoRejeicao;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;
    private final UUID ordemServicoId;
    private final UUID elaboradoPorId;
    private final List<ItemOrcamentoServico> itensServico;
    private final List<ItemOrcamentoPeca> itensPeca;

    private Orcamento(UUID id, int versao, StatusOrcamento status, Long versionLock,
                      BigDecimal valorMaoObra, BigDecimal valorPecas, BigDecimal valorAcrescimo,
                      BigDecimal valorTotal, String condicoesPagamento, Integer prazoExecucaoDias,
                      LocalDate dataValidade, String observacoes, LocalDateTime dataEnvio,
                      LocalDateTime dataRespostaCliente, String motivoRejeicao, LocalDateTime criadoEm,
                      LocalDateTime atualizadoEm, UUID ordemServicoId, UUID elaboradoPorId,
                      List<ItemOrcamentoServico> itensServico, List<ItemOrcamentoPeca> itensPeca) {
        if (ordemServicoId == null) {
            throw new BusinessException("Orçamento deve referenciar uma ordem de serviço");
        }
        if (versao <= 0) {
            throw new BusinessException("Versão do orçamento deve ser positiva");
        }
        this.id = id;
        this.versao = versao;
        this.status = status;
        this.versionLock = versionLock;
        this.valorMaoObra = zeroSeNulo(valorMaoObra);
        this.valorPecas = zeroSeNulo(valorPecas);
        this.valorAcrescimo = zeroSeNulo(valorAcrescimo);
        this.valorTotal = zeroSeNulo(valorTotal);
        this.condicoesPagamento = condicoesPagamento;
        this.prazoExecucaoDias = prazoExecucaoDias;
        this.dataValidade = dataValidade;
        this.observacoes = observacoes;
        this.dataEnvio = dataEnvio;
        this.dataRespostaCliente = dataRespostaCliente;
        this.motivoRejeicao = motivoRejeicao;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.ordemServicoId = ordemServicoId;
        this.elaboradoPorId = elaboradoPorId;
        this.itensServico = itensServico == null ? new ArrayList<>() : new ArrayList<>(itensServico);
        this.itensPeca = itensPeca == null ? new ArrayList<>() : new ArrayList<>(itensPeca);
    }

    /**
     * Nova versão de orçamento em RASCUNHO. A versão é calculada pelo caso de uso a partir da
     * maior versão já existente para a OS.
     */
    public static Orcamento criar(UUID ordemServicoId, int versao, String condicoesPagamento,
                                  Integer prazoExecucaoDias, LocalDate dataValidade, String observacoes,
                                  UUID elaboradoPorId) {
        return new Orcamento(null, versao, StatusOrcamento.RASCUNHO, null, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, condicoesPagamento, prazoExecucaoDias,
                dataValidade, observacoes, null, null, null, null, null, ordemServicoId, elaboradoPorId,
                new ArrayList<>(), new ArrayList<>());
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static Orcamento reconstituir(UUID id, int versao, StatusOrcamento status, Long versionLock,
                                         BigDecimal valorMaoObra, BigDecimal valorPecas,
                                         BigDecimal valorAcrescimo, BigDecimal valorTotal,
                                         String condicoesPagamento, Integer prazoExecucaoDias,
                                         LocalDate dataValidade, String observacoes,
                                         LocalDateTime dataEnvio, LocalDateTime dataRespostaCliente,
                                         String motivoRejeicao, LocalDateTime criadoEm,
                                         LocalDateTime atualizadoEm, UUID ordemServicoId,
                                         UUID elaboradoPorId, List<ItemOrcamentoServico> itensServico,
                                         List<ItemOrcamentoPeca> itensPeca) {
        return new Orcamento(id, versao, status, versionLock, valorMaoObra, valorPecas, valorAcrescimo,
                valorTotal, condicoesPagamento, prazoExecucaoDias, dataValidade, observacoes, dataEnvio,
                dataRespostaCliente, motivoRejeicao, criadoEm, atualizadoEm, ordemServicoId,
                elaboradoPorId, itensServico, itensPeca);
    }

    public void adicionarItemServico(ItemOrcamentoServico item) {
        this.itensServico.add(item);
    }

    public void adicionarItemPeca(ItemOrcamentoPeca item) {
        this.itensPeca.add(item);
    }

    /** Recalcula mão de obra, peças e total a partir dos itens. */
    public void recalcular() {
        this.valorMaoObra = itensServico.stream()
                .map(ItemOrcamentoServico::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.valorPecas = itensPeca.stream()
                .map(ItemOrcamentoPeca::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.valorTotal = valorMaoObra.add(valorPecas).add(valorAcrescimo);
    }

    /** Marca como enviado ao cliente e registra a data. */
    public void enviar(LocalDateTime agora) {
        if (this.status != StatusOrcamento.RASCUNHO) {
            throw new BusinessException(
                    "Apenas orçamentos em Rascunho podem ser enviados. Atual: " + status);
        }
        this.status = StatusOrcamento.ENVIADA;
        this.dataEnvio = agora;
    }

    /** Aprovação pelo cliente. */
    public void aprovar(LocalDateTime agora) {
        if (this.status != StatusOrcamento.ENVIADA) {
            throw new BusinessException(
                    "Apenas orçamentos enviados podem ser aprovados. Atual" + status);
        }
        this.status = StatusOrcamento.APROVADA;
        this.dataRespostaCliente = agora;
    }

    /** Rejeição pelo cliente, com motivo opcional. Uma nova versão pode então ser criada. */
    public void rejeitar(String motivo, LocalDateTime agora) {
        if (this.status != StatusOrcamento.ENVIADA) {
            throw new BusinessException(
                    "Apenas orçamentos enviados podem ser rejeitados. Atual:" + status);
        }
        this.status = StatusOrcamento.CANCELADA;
        this.dataRespostaCliente = agora;
        this.motivoRejeicao = motivo;
    }

    public boolean estaExpirado(LocalDate hoje) {
        return dataValidade != null && dataValidade.isBefore(hoje);
    }

    /** Ainda aguardando resposta e dentro da validade. */
    public boolean estaValido(LocalDate hoje) {
        return status == StatusOrcamento.ENVIADA && !estaExpirado(hoje);
    }

    /** Marca como expirado, se aplicável. Sem efeito em outros status. */
    public void expirar(LocalDate hoje) {
        if (this.status != StatusOrcamento.ENVIADA || !estaExpirado(hoje)) {
            return;
        }
        this.status = StatusOrcamento.EXPIRADO;
    }

    public boolean isNovo() {
        return id == null;
    }

    public boolean isAprovado() {
        return status == StatusOrcamento.APROVADA;
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public UUID getId() {
        return id;
    }

    public int getVersao() {
        return versao;
    }

    public StatusOrcamento getStatus() {
        return status;
    }

    public Long getVersionLock() {
        return versionLock;
    }

    public BigDecimal getValorMaoObra() {
        return valorMaoObra;
    }

    public BigDecimal getValorPecas() {
        return valorPecas;
    }

    public BigDecimal getValorAcrescimo() {
        return valorAcrescimo;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getCondicoesPagamento() {
        return condicoesPagamento;
    }

    public Integer getPrazoExecucaoDias() {
        return prazoExecucaoDias;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public LocalDateTime getDataRespostaCliente() {
        return dataRespostaCliente;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public UUID getOrdemServicoId() {
        return ordemServicoId;
    }

    public UUID getElaboradoPorId() {
        return elaboradoPorId;
    }

    /** Itens de serviço em lista imutável — use adicionarItemServico para incluir. */
    public List<ItemOrcamentoServico> getItensServico() {
        return List.copyOf(itensServico);
    }

    /** Itens de peça em lista imutável — use adicionarItemPeca para incluir. */
    public List<ItemOrcamentoPeca> getItensPeca() {
        return List.copyOf(itensPeca);
    }
}
