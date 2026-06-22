package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.ItemPecaOS;
import com.autopecas.autopecas.domain.entity.Mecanico;
import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.entity.Peca;
import com.autopecas.autopecas.domain.enums.StatusItemOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ItemPecaOSBuilder {

    public static ItemPecaOS.ItemPecaOSBuilder itemPecaOS(OrdemServico ordemServico, Peca peca) {
        return ItemPecaOS.builder()
                .ordemServico(ordemServico)
                .peca(peca)
                .quantidade(1)
                .precoUnitario(peca.getPrecoVenda())
                .status(StatusItemOS.PENDENTE)
                .instaladoPor(null)
                .dataInstalacao(null);
    }

    public static ItemPecaOS.ItemPecaOSBuilder itemPecaOS(OrdemServico ordemServico, Peca peca, int quantidade, BigDecimal precoUnitario) {
        return ItemPecaOS.builder()
                .ordemServico(ordemServico)
                .peca(peca)
                .quantidade(quantidade)
                .precoUnitario(precoUnitario)
                .status(StatusItemOS.PENDENTE)
                .instaladoPor(null)
                .dataInstalacao(null);
    }

    public static ItemPecaOS.ItemPecaOSBuilder itemPecaOSCompleto(OrdemServico ordemServico, Peca peca, Mecanico mecanico) {
        return ItemPecaOS.builder()
                .ordemServico(ordemServico)
                .peca(peca)
                .quantidade(1)
                .precoUnitario(peca.getPrecoVenda())
                .status(StatusItemOS.CONCLUIDO)
                .instaladoPor(mecanico)
                .dataInstalacao(LocalDateTime.now().minusHours(1));
    }
}
