package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.Atendente;
import com.autopecas.autopecas.domain.entity.Orcamento;
import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.enums.StatusOrcamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

public class OrcamentoBuilder {

    public static Orcamento.OrcamentoBuilder orcamento(OrdemServico ordemServico) {
        return Orcamento.builder()
                .id(UUID.randomUUID())
                .versao(1)
                .status(StatusOrcamento.RASCUNHO)
                .valorMaoObra(BigDecimal.ZERO)
                .valorPecas(BigDecimal.ZERO)
                .valorAcrescimo(BigDecimal.ZERO)
                .valorTotal(BigDecimal.ZERO)
                .condicoesPagamento("Pagamento em até 3x sem juros")
                .prazoExecucaoDias(5)
                .dataValidade(LocalDate.now().plusDays(7))
                .observacoes("Orçamento inicial")
                .ordemServico(ordemServico)
                .elaboradoPor(null) // Pode ser setado posteriormente
                .itensServico(new ArrayList<>())
                .itensPeca(new ArrayList<>());
    }

    public static Orcamento.OrcamentoBuilder orcamento(OrdemServico ordemServico, Atendente elaboradoPor) {
        return orcamento(ordemServico)
                .elaboradoPor(elaboradoPor);
    }
}
