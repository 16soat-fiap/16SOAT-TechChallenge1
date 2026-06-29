package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.config.SecurityConfig;
import com.autopecas.autopecas.config.WebMvcSecurityTestConfig;
import com.autopecas.autopecas.dto.orcamento.AprovarRejeitarDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoCreateDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoResponseDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.service.OrcamentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrcamentoController.class)
@Import({SecurityConfig.class, WebMvcSecurityTestConfig.class})
@DisplayName("OrcamentoController")
class OrcamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrcamentoService orcamentoService;

    @Nested
    @DisplayName("Testes dos endpoints")
    class EndpointTests {

        @Test
        @DisplayName("POST /api/ordens-servico/{osId}/orcamentos deve criar orçamento")
        void postApiOrdensServicoOsIdOrcamentosDeveCriarOrcamento() throws Exception {
            // Given
            UUID osId = UUID.randomUUID();
            OrcamentoCreateDTO request = new OrcamentoCreateDTO(
                    List.of(new OrcamentoCreateDTO.ItemServicoDTO(UUID.randomUUID(), 1)),
                    List.of(new OrcamentoCreateDTO.ItemPecaDTO(UUID.randomUUID(), 2)),
                    "Cartão",
                    3,
                    LocalDate.now().plusDays(7),
                    "Observação"
            );
            OrcamentoResponseDTO response = responseDTO();
            when(orcamentoService.criarOrcamento(osId, request)).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/ordens-servico/{osId}/orcamentos", osId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"itensServico\": [{\"servicoId\": \"%s\", \"quantidade\": 1}],
                                      \"itensPeca\": [{\"pecaId\": \"%s\", \"quantidade\": 2}],
                                      \"condicoesPagamento\": \"Cartão\",
                                      \"prazoExecucaoDias\": 3,
                                      \"dataValidade\": \"%s\",
                                      \"observacoes\": \"Observação\"
                                    }
                                    """.formatted(request.itensServico().getFirst().servicoId(), request.itensPeca().getFirst().pecaId(), request.dataValidade())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(response.status()))
                    .andExpect(jsonPath("$.valorTotal").value(150.0));
        }

        @Test
        @DisplayName("GET /api/ordens-servico/{osId}/orcamentos deve listar orçamentos")
        void getApiOrdensServicoOsIdOrcamentosDeveListarOrcamentos() throws Exception {
            // Given
            UUID osId = UUID.randomUUID();
            OrcamentoResponseDTO response = responseDTO();
            when(orcamentoService.listar(osId)).thenReturn(List.of(response));

            // When / Then
            mockMvc.perform(get("/api/ordens-servico/{osId}/orcamentos", osId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(response.id().toString()))
                    .andExpect(jsonPath("$[0].status").value(response.status()));
        }

        @Test
        @DisplayName("PATCH /api/ordens-servico/{osId}/orcamentos/{id}/enviar deve enviar orçamento")
        void patchApiOrdensServicoOsIdOrcamentosIdEnviarDeveEnviarOrcamento() throws Exception {
            // Given
            UUID osId = UUID.randomUUID();
            UUID id = UUID.randomUUID();
            OrcamentoResponseDTO response = responseDTO();
            when(orcamentoService.enviar(osId, id)).thenReturn(response);

            // When / Then
            mockMvc.perform(patch("/api/ordens-servico/{osId}/orcamentos/{id}/enviar", osId, id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(response.status()));
        }

        @Test
        @DisplayName("PATCH /api/ordens-servico/{osId}/orcamentos/{id}/aprovar deve mapear not found para 404")
        void patchApiOrdensServicoOsIdOrcamentosIdAprovarDeveMapearNotFoundPara404() throws Exception {
            // Given
            UUID osId = UUID.randomUUID();
            UUID id = UUID.randomUUID();
            when(orcamentoService.aprovar(osId, id)).thenThrow(new ResourceNotFoundException("Orcamento não encontrado"));

            // When / Then
            mockMvc.perform(patch("/api/ordens-servico/{osId}/orcamentos/{id}/aprovar", osId, id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Orcamento não encontrado"));
        }

        @Test
        @DisplayName("PATCH /api/ordens-servico/{osId}/orcamentos/{id}/rejeitar deve aceitar body opcional")
        void patchApiOrdensServicoOsIdOrcamentosIdRejeitarDeveAceitarBodyOpcional() throws Exception {
            // Given
            UUID osId = UUID.randomUUID();
            UUID id = UUID.randomUUID();
            OrcamentoResponseDTO response = responseDTO();
            when(orcamentoService.rejeitar(osId, id, new AprovarRejeitarDTO(null))).thenReturn(response);

            // When / Then
            mockMvc.perform(patch("/api/ordens-servico/{osId}/orcamentos/{id}/rejeitar", osId, id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(response.status()));

            verify(orcamentoService).rejeitar(osId, id, new AprovarRejeitarDTO(null));
        }

        @Test
        @DisplayName("PATCH /api/ordens-servico/{osId}/orcamentos/{id}/rejeitar deve mapear exceção de negócio para 422")
        void patchApiOrdensServicoOsIdOrcamentosIdRejeitarDeveMapearExcecaoDeNegocioPara422() throws Exception {
            // Given
            UUID osId = UUID.randomUUID();
            UUID id = UUID.randomUUID();
            AprovarRejeitarDTO request = new AprovarRejeitarDTO("Motivo");
            when(orcamentoService.rejeitar(osId, id, request)).thenThrow(new BusinessException("Apenas orçamentos enviados podem ser rejeitados."));

            // When / Then
            mockMvc.perform(patch("/api/ordens-servico/{osId}/orcamentos/{id}/rejeitar", osId, id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"motivo\": \"Motivo\"
                                    }
                                    """))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.error").value("Regra de negócio violada"))
                    .andExpect(jsonPath("$.message").value("Apenas orçamentos enviados podem ser rejeitados."));
        }
    }

    private OrcamentoResponseDTO responseDTO() {
        return new OrcamentoResponseDTO(
                UUID.randomUUID(),
                1,
                "ENVIADA",
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                new BigDecimal("150.00"),
                "Cartão",
                3,
                LocalDate.now().plusDays(5),
                null
        );
    }
}



