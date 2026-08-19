package com.autopecas.autopecas.domain.model.estoque;

import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro imutável de uma movimentação no estoque de uma peça.
 *
 * <p>Regras:
 * <ul>
 *   <li>Registros nunca são alterados nem excluídos.</li>
 *   <li>A quantidade é sempre positiva — o sentido vem do tipo.</li>
 *   <li>saldoApos guarda o saldo da peça depois de aplicar esta movimentação.</li>
 *   <li>ordemServicoId é preenchido quando a saída decorre de uma OS.</li>
 * </ul>
 */
public final class MovimentacaoEstoque {

    private final UUID id;
    private final UUID pecaId;
    private final TipoMovimentacaoEstoque tipo;
    private final int quantidade;
    private final int saldoApos;
    private final BigDecimal valorUnitarioMomento;
    private final String motivo;
    private final UUID ordemServicoId;
    private final UUID executadoPorId;
    private final LocalDateTime criadoEm;

    private MovimentacaoEstoque(UUID id, UUID pecaId, TipoMovimentacaoEstoque tipo, int quantidade,
                                int saldoApos, BigDecimal valorUnitarioMomento, String motivo,
                                UUID ordemServicoId, UUID executadoPorId, LocalDateTime criadoEm) {
        if (pecaId == null) {
            throw new BusinessException("Movimentação deve referenciar uma peça");
        }
        if (tipo == null) {
            throw new BusinessException("Tipo de movimentação é obrigatório");
        }
        if (quantidade <= 0) {
            throw new BusinessException("Quantidade da movimentação deve ser positiva");
        }
        this.id = id;
        this.pecaId = pecaId;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.saldoApos = saldoApos;
        this.valorUnitarioMomento = valorUnitarioMomento;
        this.motivo = motivo;
        this.ordemServicoId = ordemServicoId;
        this.executadoPorId = executadoPorId;
        this.criadoEm = criadoEm;
    }

    /** Nova movimentação, ainda sem id. Criada pelo MovimentadorDeEstoque. */
    public static MovimentacaoEstoque registrar(UUID pecaId, TipoMovimentacaoEstoque tipo, int quantidade,
                                                int saldoApos, String motivo, UUID ordemServicoId,
                                                UUID executadoPorId) {
        return new MovimentacaoEstoque(null, pecaId, tipo, quantidade, saldoApos, null, motivo,
                ordemServicoId, executadoPorId, null);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static MovimentacaoEstoque reconstituir(UUID id, UUID pecaId, TipoMovimentacaoEstoque tipo,
                                                   int quantidade, int saldoApos,
                                                   BigDecimal valorUnitarioMomento, String motivo,
                                                   UUID ordemServicoId, UUID executadoPorId,
                                                   LocalDateTime criadoEm) {
        return new MovimentacaoEstoque(id, pecaId, tipo, quantidade, saldoApos, valorUnitarioMomento,
                motivo, ordemServicoId, executadoPorId, criadoEm);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPecaId() {
        return pecaId;
    }

    public TipoMovimentacaoEstoque getTipo() {
        return tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public int getSaldoApos() {
        return saldoApos;
    }

    public BigDecimal getValorUnitarioMomento() {
        return valorUnitarioMomento;
    }

    public String getMotivo() {
        return motivo;
    }

    public UUID getOrdemServicoId() {
        return ordemServicoId;
    }

    public UUID getExecutadoPorId() {
        return executadoPorId;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
