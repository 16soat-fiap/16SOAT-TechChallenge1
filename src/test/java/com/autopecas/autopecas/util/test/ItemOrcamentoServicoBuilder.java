package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.ItemOrcamentoServico;
import com.autopecas.autopecas.domain.entity.Orcamento;
import com.autopecas.autopecas.domain.entity.Servico;

import java.math.BigDecimal;

public class ItemOrcamentoServicoBuilder {

    public static ItemOrcamentoServico.ItemOrcamentoServicoBuilder itemOrcamentoServico(Orcamento orcamento, Servico servico) {
        return ItemOrcamentoServico.builder()
                .orcamento(orcamento)
                .servico(servico)
                .quantidade(1)
                .precoUnitario(servico.getPrecoBase())
                .observacao("Serviço padrão");
    }

    public static ItemOrcamentoServico.ItemOrcamentoServicoBuilder itemOrcamentoServico(Orcamento orcamento, Servico servico, int quantidade, BigDecimal precoUnitario, String observacao) {
        return ItemOrcamentoServico.builder()
                .orcamento(orcamento)
                .servico(servico)
                .quantidade(quantidade)
                .precoUnitario(precoUnitario)
                .observacao(observacao);
    }
}
