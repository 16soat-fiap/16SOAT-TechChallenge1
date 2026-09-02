package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.port.in.ConsultaDeIndicadores;
import com.autopecas.autopecas.application.port.in.view.IndicadoresView;
import com.autopecas.autopecas.application.port.in.view.TempoMedioView;
import com.autopecas.autopecas.application.port.out.ConsultaIndicadores;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.model.os.OrdemServico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Casos de uso do dashboard.
 *
 * <p>Trabalha sobre projeções de leitura, não sobre agregados: a query port devolve as datas de
 * execução e o cálculo do tempo reaproveita a regra do domínio.
 */
public class ConsultaDeIndicadoresUseCase implements ConsultaDeIndicadores {

    private final ConsultaIndicadores consultaIndicadores;

    public ConsultaDeIndicadoresUseCase(ConsultaIndicadores consultaIndicadores) {
        this.consultaIndicadores = consultaIndicadores;
    }

    @Override
    public IndicadoresView indicadores() {
        return new IndicadoresView(
                consultaIndicadores.contarOrdensPorStatus(StatusOS.RECEBIDA),
                consultaIndicadores.contarOrdensPorStatus(StatusOS.EM_EXECUCAO),
                consultaIndicadores.contarOrdensPorStatus(StatusOS.FINALIZADA),
                consultaIndicadores.contarOrdensPorStatus(StatusOS.ENTREGUE),
                consultaIndicadores.contarPecasComEstoqueBaixo());
    }

    @Override
    public List<TempoMedioView> tempoMedioDeExecucao() {
        List<ConsultaIndicadores.ExecucaoDeMecanico> execucoes = consultaIndicadores.execucoesConcluidas();

        Map<UUID, List<Long>> temposPorMecanico = new LinkedHashMap<>();
        for (ConsultaIndicadores.ExecucaoDeMecanico execucao : execucoes) {
            Long tempo = OrdemServico.calcularTempoExecucaoMinutos(
                    execucao.dataInicioExecucao(), execucao.dataFinalizacao());
            if (tempo != null) {
                temposPorMecanico.computeIfAbsent(execucao.mecanicoId(), id -> new ArrayList<>())
                        .add(tempo);
            }
        }

        List<TempoMedioView> resultado = new ArrayList<>();
        Map<UUID, Boolean> jaIncluido = new LinkedHashMap<>();
        for (ConsultaIndicadores.ExecucaoDeMecanico execucao : execucoes) {
            List<Long> tempos = temposPorMecanico.get(execucao.mecanicoId());
            if (tempos == null || tempos.isEmpty() || jaIncluido.containsKey(execucao.mecanicoId())) {
                continue;
            }
            double media = tempos.stream().mapToLong(Long::longValue).average().orElse(0.0);
            resultado.add(new TempoMedioView(execucao.mecanicoId(), execucao.mecanicoNome(), media));
            jaIncluido.put(execucao.mecanicoId(), Boolean.TRUE);
        }

        return resultado;
    }
}
