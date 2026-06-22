package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.config.SecurityConfig;
import com.autopecas.autopecas.dto.dashboard.DashboardDTO;
import com.autopecas.autopecas.dto.dashboard.TempoMedioDTO;
import com.autopecas.autopecas.service.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
@DisplayName("DashboardController")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Nested
    @DisplayName("Testes dos endpoints")
    class EndpointTests {

        @Test
        @DisplayName("GET /api/dashboard deve retornar indicadores")
        void getApiDashboardDeveRetornarIndicadores() throws Exception {
            // Given
            DashboardDTO response = new DashboardDTO(4, 2, 3, 8, 5);
            when(dashboardService.obterDashboard()).thenReturn(response);

            // When / Then
            mockMvc.perform(get("/api/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalOsAbertas").value(4))
                    .andExpect(jsonPath("$.estoqueBaixoCount").value(5));
        }

        @Test
        @DisplayName("GET /api/dashboard/tempo-medio-execucao deve retornar tempos médios")
        void getApiDashboardTempoMedioExecucaoDeveRetornarTemposMedios() throws Exception {
            // Given
            TempoMedioDTO response = new TempoMedioDTO(UUID.randomUUID(), "Mecânico Teste", 75.0);
            when(dashboardService.tempoMedioExecucao()).thenReturn(List.of(response));

            // When / Then
            mockMvc.perform(get("/api/dashboard/tempo-medio-execucao"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].mecanicoNome").value("Mecânico Teste"))
                    .andExpect(jsonPath("$[0].tempoMedioMinutos").value(75.0));
        }
    }
}


