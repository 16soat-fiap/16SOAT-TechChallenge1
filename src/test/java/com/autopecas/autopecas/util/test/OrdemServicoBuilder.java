package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.Atendente;
import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.Mecanico;
import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.entity.Veiculo;
import com.autopecas.autopecas.domain.enums.StatusOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class OrdemServicoBuilder {

    public static OrdemServico.OrdemServicoBuilder ordemServico(Cliente cliente, Veiculo veiculo, Atendente atendenteRecepcao) {
        return OrdemServico.builder()
                .id(UUID.randomUUID())
                .numero(null)
                .version(null)
                .status(StatusOS.RECEBIDA)
                .quilometragemEntrada(50000)
                .observacoesEntrada(null)
                .diagnostico(null)
                .queixaCliente("Barulho estranho no motor")
                .valorTotalAprovado(BigDecimal.ZERO)
                .dataInicioExecucao(null)
                .dataFinalizacao(null)
                .dataEntrega(null)
                .cliente(cliente)
                .veiculo(veiculo)
                .atendenteRecepcao(atendenteRecepcao)
                .mecanicoResponsavel(null)
                .atendenteEntrega(null);
    }

    public static OrdemServico.OrdemServicoBuilder ordemServicoCompleta(Cliente cliente, Veiculo veiculo, Atendente atendenteRecepcao, Mecanico mecanicoResponsavel, Atendente atendenteEntrega) {
        return OrdemServico.builder()
                .id(UUID.randomUUID())
                .numero("OS-000001")
                .version(1L)
                .status(StatusOS.FINALIZADA)
                .quilometragemEntrada(50000)
                .observacoesEntrada("Cliente ciente do desgaste dos pneus.")
                .diagnostico("Rolamento da roda dianteira direita com folga.")
                .queixaCliente("Barulho estranho no motor")
                .valorTotalAprovado(new BigDecimal("500.00"))
                .dataInicioExecucao(LocalDateTime.now().minusDays(2))
                .dataFinalizacao(LocalDateTime.now().minusDays(1))
                .dataEntrega(null)
                .quilometragemEntrada(50000)
                .cliente(cliente)
                .veiculo(veiculo)
                .atendenteRecepcao(atendenteRecepcao)
                .mecanicoResponsavel(mecanicoResponsavel)
                .atendenteEntrega(atendenteEntrega);
    }
}
