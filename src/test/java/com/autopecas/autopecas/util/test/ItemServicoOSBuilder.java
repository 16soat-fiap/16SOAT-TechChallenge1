package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.ItemServicoOS;
import com.autopecas.autopecas.domain.entity.Mecanico;
import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.entity.Servico;
import com.autopecas.autopecas.domain.enums.StatusItemOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ItemServicoOSBuilder {

    public static ItemServicoOS.ItemServicoOSBuilder itemServicoOS(OrdemServico ordemServico, Servico servico) {
        return ItemServicoOS.builder()
                .ordemServico(ordemServico)
                .servico(servico)
                .quantidade(1)
                .precoUnitario(servico.getPrecoBase())
                .status(StatusItemOS.PENDENTE)
                .executadoPor(null)
                .dataInicioExecucao(null)
                .dataFimExecucao(null)
                .observacao("Serviço a ser executado");
    }

    public static ItemServicoOS.ItemServicoOSBuilder itemServicoOS(OrdemServico ordemServico, Servico servico, int quantidade, BigDecimal precoUnitario, String observacao) {
        return ItemServicoOS.builder()
                .ordemServico(ordemServico)
                .servico(servico)
                .quantidade(quantidade)
                .precoUnitario(precoUnitario)
                .status(StatusItemOS.PENDENTE)
                .executadoPor(null)
                .dataInicioExecucao(null)
                .dataFimExecucao(null)
                .observacao(observacao);
    }

    public static ItemServicoOS.ItemServicoOSBuilder itemServicoOSCompleto(OrdemServico ordemServico, Servico servico, Mecanico mecanico) {
        return ItemServicoOS.builder()
                .ordemServico(ordemServico)
                .servico(servico)
                .quantidade(1)
                .precoUnitario(servico.getPrecoBase())
                .status(StatusItemOS.CONCLUIDO)
                .executadoPor(mecanico)
                .dataInicioExecucao(LocalDateTime.now().minusHours(2))
                .dataFimExecucao(LocalDateTime.now().minusHours(1))
                .observacao("Serviço concluído com sucesso");
    }
}
