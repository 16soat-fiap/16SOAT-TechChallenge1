package com.autopecas.autopecas.domain.model.os;

import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.model.funcionario.Funcionario;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro imutável de uma transição de status de uma OS.
 *
 * <p>Regras:
 * <ul>
 *   <li>Registros nunca são alterados nem excluídos.</li>
 *   <li>Toda transição gera um novo registro.</li>
 *   <li>statusAnterior é nulo no registro de abertura.</li>
 * </ul>
 */
public final class HistoricoStatusOS {

    private final Long id;
    private final UUID ordemServicoId;
    private final StatusOS statusAnterior;
    private final StatusOS statusNovo;
    private final String observacao;
    private final String alteradoPor;
    private final UUID executadoPorId;
    private final LocalDateTime criadoEm;

    private HistoricoStatusOS(Long id, UUID ordemServicoId, StatusOS statusAnterior, StatusOS statusNovo,
                              String observacao, String alteradoPor, UUID executadoPorId,
                              LocalDateTime criadoEm) {
        if (ordemServicoId == null) {
            throw new BusinessException("Histórico deve referenciar uma ordem de serviço");
        }
        if (statusNovo == null) {
            throw new BusinessException("Status novo é obrigatório no histórico");
        }
        if (alteradoPor == null || alteradoPor.isBlank()) {
            throw new BusinessException("Autor da alteração é obrigatório no histórico");
        }
        this.id = id;
        this.ordemServicoId = ordemServicoId;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.observacao = observacao;
        this.alteradoPor = alteradoPor;
        this.executadoPorId = executadoPorId;
        this.criadoEm = criadoEm;
    }

    /** Registro de abertura da OS, sem status anterior. */
    public static HistoricoStatusOS abertura(UUID ordemServicoId, Funcionario atendente) {
        return new HistoricoStatusOS(null, ordemServicoId, null, StatusOS.RECEBIDA,
                "Ordem de serviço aberta.", atendente.getIdentificacaoComTipo(), atendente.getId(), null);
    }

    /** Registro de transição executada por um funcionário identificado. */
    public static HistoricoStatusOS porFuncionario(UUID ordemServicoId, StatusOS anterior, StatusOS novo,
                                                   String observacao, Funcionario funcionario) {
        return new HistoricoStatusOS(null, ordemServicoId, anterior, novo, observacao,
                funcionario.getIdentificacaoComTipo(), funcionario.getId(), null);
    }

    /** Registro de transição automática feita pelo sistema. */
    public static HistoricoStatusOS porSistema(UUID ordemServicoId, StatusOS anterior, StatusOS novo,
                                               String observacao) {
        return new HistoricoStatusOS(null, ordemServicoId, anterior, novo, observacao, "SISTEMA", null, null);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static HistoricoStatusOS reconstituir(Long id, UUID ordemServicoId, StatusOS statusAnterior,
                                                 StatusOS statusNovo, String observacao, String alteradoPor,
                                                 UUID executadoPorId, LocalDateTime criadoEm) {
        return new HistoricoStatusOS(id, ordemServicoId, statusAnterior, statusNovo, observacao,
                alteradoPor, executadoPorId, criadoEm);
    }

    public Long getId() {
        return id;
    }

    public UUID getOrdemServicoId() {
        return ordemServicoId;
    }

    public StatusOS getStatusAnterior() {
        return statusAnterior;
    }

    public StatusOS getStatusNovo() {
        return statusNovo;
    }

    public String getObservacao() {
        return observacao;
    }

    public String getAlteradoPor() {
        return alteradoPor;
    }

    public UUID getExecutadoPorId() {
        return executadoPorId;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
