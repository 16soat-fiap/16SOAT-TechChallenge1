package com.autopecas.autopecas.util.test;
import com.autopecas.autopecas.domain.entity.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.entity.Peca;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import java.math.BigDecimal;
import java.util.UUID;
public class MovimentacaoEstoqueBuilder {
    public static MovimentacaoEstoque.MovimentacaoEstoqueBuilder movimentacaoEstoque(Peca peca) {
        return MovimentacaoEstoque.builder()
                .id(UUID.randomUUID())
                .peca(peca)
                .tipo(TipoMovimentacaoEstoque.ENTRADA)
                .quantidade(10)
                .saldoApos(10)
                .valorUnitarioMomento(new BigDecimal("50.00"))
                .motivo("Compra de reposicao")
                .ordemServico(null)
                .executadoPor(null);
    }
    public static MovimentacaoEstoque.MovimentacaoEstoqueBuilder movimentacaoSaida(Peca peca) {
        return MovimentacaoEstoque.builder()
                .id(UUID.randomUUID())
                .peca(peca)
                .tipo(TipoMovimentacaoEstoque.SAIDA)
                .quantidade(3)
                .saldoApos(7)
                .valorUnitarioMomento(new BigDecimal("50.00"))
                .motivo("Uso em OS")
                .ordemServico(null)
                .executadoPor(null);
    }
}
