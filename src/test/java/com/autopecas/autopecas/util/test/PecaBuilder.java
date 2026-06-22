package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.Peca;

import java.math.BigDecimal;
import java.util.UUID;

public class PecaBuilder {

    public static Peca.PecaBuilder peca() {
        return Peca.builder()
                .id(UUID.randomUUID())
                .codigo("PEC001")
                .nome("Peça Teste")
                .descricao("Peça de teste para cenários unitários")
                .marca("Marca Teste")
                .precoVenda(new BigDecimal("100.00"))
                .quantidadeEstoque(10)
                .quantidadeMinima(2)
                .unidade("un")
                .ativo(true);
    }
}

