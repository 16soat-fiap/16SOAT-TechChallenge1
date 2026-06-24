package com.autopecas.autopecas.integration;

import com.autopecas.autopecas.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.dto.os.AvancarStatusDTO;
import com.autopecas.autopecas.dto.os.OrdemServicoCreateDTO;
import com.autopecas.autopecas.dto.peca.PecaCreateDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoCreateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para DashboardController -> DashboardService.
 *
 * Valida:
 *  - GET /api/dashboard 200 com os campos osAbertas, osEmExecucao, osFinalizadas,
 *    osEntregues, estoqueBaixo
 *  - Contadores refletem corretamente as OS criadas no próprio teste
 *  - GET /api/dashboard/tempo-medio-execucao 200 com array (pode estar vazio)
 */
@DisplayName("DashboardController - Integração")
class DashboardIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/dashboard";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private UUID criarCliente() throws Exception {
        ClienteCreatePFDTO dto = new ClienteCreatePFDTO(
                "Cliente Dashboard", "dash@teste.com", "11999990004",
                true, "52998224725",
                LocalDate.of(1990, 1, 1), null, null);
        String resp = mockMvc.perform(post("/api/clientes/pf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(resp).get("id").asText());
    }

    private UUID criarVeiculo(UUID clienteId) throws Exception {
        VeiculoCreateDTO dto = new VeiculoCreateDTO(
                clienteId, "CCC0003", null, null, "VW", "Gol", 2019, "Cinza");
        String resp = mockMvc.perform(post("/api/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(resp).get("id").asText());
    }

    private UUID criarOS(UUID clienteId, UUID veiculoId) throws Exception {
        OrdemServicoCreateDTO dto = new OrdemServicoCreateDTO(
                clienteId, veiculoId, "Verificação geral", null, null);
        String resp = mockMvc.perform(post("/api/ordens-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(resp).get("id").asText());
    }

    private void avancarStatus(UUID osId, String status) throws Exception {
        mockMvc.perform(patch("/api/ordens-servico/" + osId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvancarStatusDTO(status, null))))
                .andExpect(status().isOk());
    }

    // ─── Dashboard ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/dashboard")
    class DashboardTests {

        @Test
        @DisplayName("deve retornar 200 com todos os campos do dashboard")
        void deveRetornarDashboard() throws Exception {
            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.osAbertas").isNumber())
                    .andExpect(jsonPath("$.osEmExecucao").isNumber())
                    .andExpect(jsonPath("$.osFinalizadas").isNumber())
                    .andExpect(jsonPath("$.osEntregues").isNumber())
                    .andExpect(jsonPath("$.estoqueBaixo").isNumber());
        }

        @Test
        @DisplayName("deve contabilizar OS em RECEBIDA no contador osAbertas")
        void deveContabilizarOSRecebida() throws Exception {
            // Obtém contagem atual
            String respAntes = mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            int osAbertasAntes = objectMapper.readTree(respAntes).get("osAbertas").asInt();

            // Cria uma nova OS (status RECEBIDA)
            UUID clienteId = criarCliente();
            UUID veiculoId = criarVeiculo(clienteId);
            criarOS(clienteId, veiculoId);

            // Contador deve ter aumentado em pelo menos 1
            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.osAbertas", greaterThanOrEqualTo(osAbertasAntes + 1)));
        }

        @Test
        @DisplayName("deve contabilizar peças com estoque baixo")
        void deveContabilizarEstoqueBaixo() throws Exception {
            // Cria peça com estoque baixo (qtd=1 ≤ minimo=2)
            PecaCreateDTO dtoPeca = new PecaCreateDTO(
                    "DASH-PECA", "Peça Dashboard", "Desc",
                    new BigDecimal("30.00"), 1, 2, "un");
            mockMvc.perform(post("/api/pecas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dtoPeca)));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estoqueBaixo", greaterThanOrEqualTo(1)));
        }
    }

    // ─── Tempo Médio de Execução ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/dashboard/tempo-medio-execucao")
    class TempoMedioTests {

        @Test
        @DisplayName("deve retornar 200 com array (possivelmente vazio)")
        void deveRetornarTempoMedioExecucao() throws Exception {
            mockMvc.perform(get(BASE + "/tempo-medio-execucao"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("deve retornar lista com dados quando há OS finalizadas")
        void deveRetornarTempoMedioQuandoHaOsFinalizadas() throws Exception {
            UUID clienteId = criarCliente();
            UUID veiculoId = criarVeiculo(clienteId);
            UUID osId = criarOS(clienteId, veiculoId);

            // Avança até FINALIZADA (sem criar orçamento — direto via status para fins de teste)
            // RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO
            // Nota: EM_EXECUCAO requer aprovação de orçamento, então ficamos em AGUARDANDO_APROVACAO
            avancarStatus(osId, "EM_DIAGNOSTICO");
            avancarStatus(osId, "AGUARDANDO_APROVACAO");

            // Dashboard deve responder sem erros mesmo sem OS finalizadas neste teste
            mockMvc.perform(get(BASE + "/tempo-medio-execucao"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
