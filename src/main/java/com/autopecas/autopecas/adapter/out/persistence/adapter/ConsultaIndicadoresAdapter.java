package com.autopecas.autopecas.adapter.out.persistence.adapter;

import com.autopecas.autopecas.adapter.out.persistence.repository.OrdemServicoJpaRepository;
import com.autopecas.autopecas.adapter.out.persistence.repository.PecaJpaRepository;
import com.autopecas.autopecas.application.port.out.ConsultaIndicadores;
import com.autopecas.autopecas.domain.enums.StatusOS;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter da query port de indicadores.
 *
 * <p>Devolve contagens agregadas pelo banco e as linhas de execução por projeção — a versão
 * anterior carregava o grafo completo das OS finalizadas para calcular o tempo médio.
 */
@Component
public class ConsultaIndicadoresAdapter implements ConsultaIndicadores {

    private static final List<StatusOS> STATUS_CONCLUIDOS =
            List.of(StatusOS.FINALIZADA, StatusOS.ENTREGUE);

    private final OrdemServicoJpaRepository ordemServicoRepository;
    private final PecaJpaRepository pecaRepository;

    public ConsultaIndicadoresAdapter(OrdemServicoJpaRepository ordemServicoRepository,
                                      PecaJpaRepository pecaRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.pecaRepository = pecaRepository;
    }

    @Override
    public long contarOrdensPorStatus(StatusOS status) {
        return ordemServicoRepository.countByStatus(status);
    }

    @Override
    public long contarPecasComEstoqueBaixo() {
        return pecaRepository.countEstoqueBaixo();
    }

    @Override
    public List<ExecucaoDeMecanico> execucoesConcluidas() {
        return ordemServicoRepository.buscarExecucoesConcluidas(STATUS_CONCLUIDOS).stream()
                .map(linha -> new ExecucaoDeMecanico(linha.mecanicoId(), linha.mecanicoNome(),
                        linha.dataInicioExecucao(), linha.dataFinalizacao()))
                .toList();
    }
}
