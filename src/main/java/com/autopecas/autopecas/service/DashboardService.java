package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.dto.dashboard.DashboardDTO;
import com.autopecas.autopecas.dto.dashboard.TempoMedioDTO;
import com.autopecas.autopecas.repository.OrdemServicoRepository;
import com.autopecas.autopecas.repository.PecaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final PecaRepository pecaRepository;

    @Transactional(readOnly = true)
    public DashboardDTO obterDashboard() {
        long totalOsAbertas     = ordemServicoRepository.countByStatus(StatusOS.RECEBIDA);
        long totalOsEmExecucao  = ordemServicoRepository.countByStatus(StatusOS.EM_EXECUCAO);
        long totalOsFinalizadas = ordemServicoRepository.countByStatus(StatusOS.FINALIZADA);
        long totalOsEntregues   = ordemServicoRepository.countByStatus(StatusOS.ENTREGUE);
        long estoqueBaixoCount  = pecaRepository.countEstoqueBaixo();

        return new DashboardDTO(
                totalOsAbertas,
                totalOsEmExecucao,
                totalOsFinalizadas,
                totalOsEntregues,
                estoqueBaixoCount
        );
    }

    @Transactional(readOnly = true)
    public List<TempoMedioDTO> tempoMedioExecucao() {
        List<OrdemServico> osFinalizadas = ordemServicoRepository
                .findByStatusIn(List.of(StatusOS.FINALIZADA, StatusOS.ENTREGUE));

        Map<UUID, List<Long>> tempoPorMecanico = osFinalizadas.stream()
                .filter(os -> os.getMecanicoResponsavel() != null)
                .filter(os -> os.calcularTempoExecucaoMinutos() != null)
                .collect(Collectors.groupingBy(
                        os -> os.getMecanicoResponsavel().getId(),
                        Collectors.mapping(OrdemServico::calcularTempoExecucaoMinutos, Collectors.toList())
                ));

        List<TempoMedioDTO> resultado = new ArrayList<>();
        for (OrdemServico os : osFinalizadas) {
            if (os.getMecanicoResponsavel() == null) continue;
            UUID mecanicoId = os.getMecanicoResponsavel().getId();
            String mecanicoNome = os.getMecanicoResponsavel().getNome();
            List<Long> tempos = tempoPorMecanico.get(mecanicoId);
            if (tempos == null || tempos.isEmpty()) continue;

            double media = tempos.stream().mapToLong(Long::longValue).average().orElse(0.0);

            boolean jaAdicionado = resultado.stream().anyMatch(t -> t.mecanicoId().equals(mecanicoId));
            if (!jaAdicionado) {
                resultado.add(new TempoMedioDTO(mecanicoId, mecanicoNome, media));
            }
        }

        return resultado;
    }
}
