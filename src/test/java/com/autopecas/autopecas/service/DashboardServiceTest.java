package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Mecanico;
import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.dto.dashboard.DashboardDTO;
import com.autopecas.autopecas.dto.dashboard.TempoMedioDTO;
import com.autopecas.autopecas.repository.OrdemServicoRepository;
import com.autopecas.autopecas.repository.PecaRepository;
import com.autopecas.autopecas.util.test.FuncionarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceTest {

    @Mock
    private OrdemServicoRepository ordemServicoRepository;
    @Mock
    private PecaRepository pecaRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Mecanico mecanico;

    @BeforeEach
    void setUp() {
        mecanico = FuncionarioBuilder.mecanico().build();
    }

    @Nested
    @DisplayName("Testes do método obterDashboard()")
    class ObterDashboardTests {

        @Test
        @DisplayName("deve retornar os indicadores agregados do dashboard")
        void deveRetornarOsIndicadoresAgregadosDoDashboard() {
            // Given
            when(ordemServicoRepository.countByStatus(StatusOS.RECEBIDA)).thenReturn(4L);
            when(ordemServicoRepository.countByStatus(StatusOS.EM_EXECUCAO)).thenReturn(2L);
            when(ordemServicoRepository.countByStatus(StatusOS.FINALIZADA)).thenReturn(3L);
            when(ordemServicoRepository.countByStatus(StatusOS.ENTREGUE)).thenReturn(8L);
            when(pecaRepository.countEstoqueBaixo()).thenReturn(5L);

            // When
            DashboardDTO resultado = dashboardService.obterDashboard();

            // Then
            assertThat(resultado.totalOsAbertas()).isEqualTo(4L);
            assertThat(resultado.totalOsEmExecucao()).isEqualTo(2L);
            assertThat(resultado.totalOsFinalizadas()).isEqualTo(3L);
            assertThat(resultado.totalOsEntregues()).isEqualTo(8L);
            assertThat(resultado.estoqueBaixoCount()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Testes do método tempoMedioExecucao()")
    class TempoMedioExecucaoTests {

        @Test
        @DisplayName("deve calcular tempo médio por mecânico usando calcularTempoDeExecucaoMinutos")
        void deveCalcularTempoMedioPorMecanicoUsandoCalcularTempoDeExecucaoMinutos() {
            // Given
            OrdemServico os1 = ordemServicoFinalizada(10, 0, 11, 0);
            OrdemServico os2 = ordemServicoFinalizada(12, 0, 14, 0);
            when(ordemServicoRepository.findByStatusIn(List.of(StatusOS.FINALIZADA, StatusOS.ENTREGUE))).thenReturn(List.of(os1, os2));

            // When
            List<TempoMedioDTO> resultado = dashboardService.tempoMedioExecucao();

            // Then
            assertThat(resultado).hasSize(1);
            assertThat(resultado.getFirst().mecanicoId()).isEqualTo(mecanico.getId());
            assertThat(resultado.getFirst().mecanicoNome()).isEqualTo(mecanico.getNome());
            assertThat(resultado.getFirst().tempoMedioMinutos()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("não deve quebrar nem distorcer a média quando há OS sem tempo registrado")
        void naoDeveQuebrarNemDistorcerAMediaQuandoHaOsSemTempoRegistrado() {
            // Given
            OrdemServico osComTempo = ordemServicoFinalizada(8, 0, 9, 0);
            OrdemServico osSemFim = OrdemServico.builder()
                    .id(UUID.randomUUID())
                    .status(StatusOS.FINALIZADA)
                    .mecanicoResponsavel(mecanico)
                    .dataInicioExecucao(LocalDateTime.of(2026, 6, 21, 10, 0))
                    .dataFinalizacao(null)
                    .build();
            OrdemServico osSemMecanico = OrdemServico.builder()
                    .id(UUID.randomUUID())
                    .status(StatusOS.ENTREGUE)
                    .dataInicioExecucao(LocalDateTime.of(2026, 6, 21, 10, 0))
                    .dataFinalizacao(LocalDateTime.of(2026, 6, 21, 11, 0))
                    .build();

            when(ordemServicoRepository.findByStatusIn(List.of(StatusOS.FINALIZADA, StatusOS.ENTREGUE)))
                    .thenReturn(List.of(osComTempo, osSemFim, osSemMecanico));

            // When
            List<TempoMedioDTO> resultado = dashboardService.tempoMedioExecucao();

            // Then
            assertThat(resultado).hasSize(1);
            assertThat(resultado.getFirst().tempoMedioMinutos()).isEqualTo(60.0);
        }
    }

    private OrdemServico ordemServicoFinalizada(int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        return OrdemServico.builder()
                .id(UUID.randomUUID())
                .status(StatusOS.FINALIZADA)
                .mecanicoResponsavel(mecanico)
                .dataInicioExecucao(LocalDateTime.of(2026, 6, 21, horaInicio, minutoInicio))
                .dataFinalizacao(LocalDateTime.of(2026, 6, 21, horaFim, minutoFim))
                .build();
    }
}

