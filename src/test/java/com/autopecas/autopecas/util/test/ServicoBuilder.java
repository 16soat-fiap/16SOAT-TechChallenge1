package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.Servico;

import java.math.BigDecimal;
import java.util.UUID;

public class ServicoBuilder {

    public static Servico.ServicoBuilder servico() {
        return Servico.builder()
                .id(UUID.randomUUID())
                .nome("Troca de Óleo")
                .descricao("Troca de óleo do motor com filtro")
                .precoBase(new BigDecimal("150.00"))
                .tempoEstimadoMinutos(60)
                .ativo(true);
    }
}
