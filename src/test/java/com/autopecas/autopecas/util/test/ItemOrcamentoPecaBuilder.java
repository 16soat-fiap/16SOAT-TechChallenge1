package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.ItemOrcamentoPeca;
import com.autopecas.autopecas.domain.entity.Orcamento;
import com.autopecas.autopecas.domain.entity.Peca;

import java.math.BigDecimal;

public class ItemOrcamentoPecaBuilder {

    public static ItemOrcamentoPeca.ItemOrcamentoPecaBuilder itemOrcamentoPeca(Orcamento orcamento, Peca peca) {
        return ItemOrcamentoPeca.builder()
                .orcamento(orcamento)
                .peca(peca)
                .quantidade(1)
                .precoUnitario(peca.getPrecoVenda());
    }

    public static ItemOrcamentoPeca.ItemOrcamentoPecaBuilder itemOrcamentoPeca(Orcamento orcamento, Peca peca, int quantidade, BigDecimal precoUnitario) {
        return ItemOrcamentoPeca.builder()
                .orcamento(orcamento)
                .peca(peca)
                .quantidade(quantidade)
                .precoUnitario(precoUnitario);
    }
}
