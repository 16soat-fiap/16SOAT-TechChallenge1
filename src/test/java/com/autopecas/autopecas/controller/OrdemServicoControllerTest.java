package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.config.SecurityConfig;
import com.autopecas.autopecas.config.WebMvcSecurityTestConfig;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.dto.os.*;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.service.OrdemServicoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdemServicoController.class)
@Import({SecurityConfig.class, WebMvcSecurityTestConfig.class})
@DisplayName("OrdemServicoController")
class OrdemServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdemServicoService ordemServicoService;

    @Nested
    @DisplayName("Testes dos endpoints")
    class EndpointTests {

        @Test
        @DisplayName("GET /api/ordens-servico deve listar com filtro opcional")
        void getApiOrdensServicoDeveListarComFiltroOpcional() throws Exception {
            // Given
            OrdemServicoResponseDTO response = responseDTO();
            when(ordemServicoService.listar(eq(StatusOS.EM_DIAGNOSTICO), eq(response.clienteId()), eq(null), any()))
                    .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

            // When / Then
            mockMvc.perform(get("/api/ordens-servico")
                            .param("status", "EM_DIAGNOSTICO")
                            .param("clienteId", response.clienteId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].numero").value(response.numero()))
                    .andExpect(jsonPath("$.content[0].status").value(response.status()))
                    .andExpect(jsonPath("$.content[0].clienteNome").value(response.clienteNome()));
        }

        @Test
        @DisplayName("GET /api/ordens-servico/{numero} deve retornar 404 quando a OS não existe")
        void getApiOrdensServicoNumeroDeveRetornar404QuandoAOsNaoExiste() throws Exception {
            // Given
            when(ordemServicoService.buscarPorNumero("OS-999999"))
                    .thenThrow(new ResourceNotFoundException("Ordem de serviço não encontrada com número: OS-999999"));

            // When / Then
            mockMvc.perform(get("/api/ordens-servico/OS-999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
                    .andExpect(jsonPath("$.message").value("Ordem de serviço não encontrada com número: OS-999999"));
        }

        @Test
        @DisplayName("POST /api/ordens-servico deve criar ordem de serviço")
        void postApiOrdensServicoDeveCriarOrdemDeServico() throws Exception {
            // Given
            OrdemServicoCreateDTO request = new OrdemServicoCreateDTO(UUID.randomUUID(), UUID.randomUUID(), "Barulho no motor", "Cliente aguardará", 123456);
            OrdemServicoResponseDTO response = responseDTO();
            when(ordemServicoService.criar(any(OrdemServicoCreateDTO.class), any(String.class))).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/ordens-servico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"clienteId\": \"%s\",
                                      \"veiculoId\": \"%s\",
                                      \"queixaCliente\": \"Barulho no motor\",
                                      \"observacoesEntrada\": \"Cliente aguardará\",
                                      \"quilometragemEntrada\": 123456
                                    }
                                    """.formatted(request.clienteId(), request.veiculoId())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.numero").value(response.numero()))
                    .andExpect(jsonPath("$.status").value(response.status()))
                    .andExpect(jsonPath("$.clienteId").value(response.clienteId().toString()));
        }

        @Test
        @DisplayName("POST /api/ordens-servico deve retornar 400 para payload inválido")
        void postApiOrdensServicoDeveRetornar400ParaPayloadInvalido() throws Exception {
            // Given
            String payload = """
                    {
                      "observacoesEntrada": "sem ids",
                      "quilometragemEntrada": 1000
                    }
                    """;

            // When / Then
            mockMvc.perform(post("/api/ordens-servico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Dados inválidos"))
                    .andExpect(jsonPath("$.campos.clienteId").value("Cliente é obrigatório"))
                    .andExpect(jsonPath("$.campos.veiculoId").value("Veículo é obrigatório"))
                    .andExpect(jsonPath("$.campos.queixaCliente").value("Queixa do cliente é obrigatória"));
        }

        @Test
        @DisplayName("PATCH /api/ordens-servico/{id}/status deve avançar status")
        void patchApiOrdensServicoIdStatusDeveAvancarStatus() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            AvancarStatusDTO request = new AvancarStatusDTO("EM_DIAGNOSTICO", "Início do diagnóstico");
            OrdemServicoResponseDTO response = responseDTO();
            when(ordemServicoService.avancarStatus(eq(id), any(AvancarStatusDTO.class), any(String.class))).thenReturn(response);

            // When / Then
            mockMvc.perform(patch("/api/ordens-servico/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"novoStatus\": \"EM_DIAGNOSTICO\",
                                      \"observacao\": \"Início do diagnóstico\"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.numero").value(response.numero()))
                    .andExpect(jsonPath("$.status").value(response.status()));
        }

        @Test
        @DisplayName("PATCH /api/ordens-servico/{id}/status deve mapear exceção de negócio para 422")
        void patchApiOrdensServicoIdStatusDeveMapearExcecaoDeNegocioPara422() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            AvancarStatusDTO request = new AvancarStatusDTO("FINALIZADA", "Pular etapas");
            when(ordemServicoService.avancarStatus(eq(id), eq(request), any(String.class)))
                    .thenThrow(new BusinessException("Transição inválida"));

            // When / Then
            mockMvc.perform(patch("/api/ordens-servico/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"novoStatus\": \"FINALIZADA\",
                                      \"observacao\": \"Pular etapas\"
                                    }
                                    """))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.error").value("Regra de negócio violada"))
                    .andExpect(jsonPath("$.message").value("Transição inválida"));
        }

        @Test
        @DisplayName("PATCH /api/ordens-servico/{id}/diagnostico deve registrar diagnóstico")
        void patchApiOrdensServicoIdDiagnosticoDeveRegistrarDiagnostico() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            DiagnosticoDTO request = new DiagnosticoDTO("Falha no sistema de arrefecimento");
            OrdemServicoResponseDTO response = responseDTO();
            when(ordemServicoService.registrarDiagnostico(eq(id), any(DiagnosticoDTO.class))).thenReturn(response);

            // When / Then
            mockMvc.perform(patch("/api/ordens-servico/{id}/diagnostico", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"diagnostico\": \"Falha no sistema de arrefecimento\"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.numero").value(response.numero()));
        }

        @Test
        @DisplayName("PATCH /api/ordens-servico/{id}/diagnostico deve retornar 400 para payload inválido")
        void patchApiOrdensServicoIdDiagnosticoDeveRetornar400ParaPayloadInvalido() throws Exception {
            // Given
            UUID id = UUID.randomUUID();

            // When / Then
            mockMvc.perform(patch("/api/ordens-servico/{id}/diagnostico", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"diagnostico\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.campos.diagnostico").value("Diagnóstico é obrigatório"));
        }

        @Test
        @DisplayName("PATCH /api/ordens-servico/{id}/mecanico com mecanicoId nulo deve retornar 400")
        void patchApiOrdensServicoIdMecanicoComMecanicoIdNuloDeveRetornar400() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            // When / Then
            mockMvc.perform(patch("/api/ordens-servico/{id}/mecanico", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mecanicoId\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Dados inválidos"))
                    .andExpect(jsonPath("$.campos.mecanicoId").value("Mecânico é obrigatório"));
        }
        @Test
        @DisplayName("PATCH /api/ordens-servico/{id}/mecanico deve atribuir mecânico com sucesso")
        void patchApiOrdensServicoIdMecanicoDeveAtribuirMecanicoComSucesso() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            AtribuirMecanicoDTO request = new AtribuirMecanicoDTO(UUID.randomUUID());
            OrdemServicoResponseDTO response = responseDTO();
            when(ordemServicoService.atribuirMecanico(eq(id), any(AtribuirMecanicoDTO.class))).thenReturn(response);
            // When / Then
            mockMvc.perform(patch("/api/ordens-servico/{id}/mecanico", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"mecanicoId\": \"%s\"
                                    }
                                    """.formatted(request.mecanicoId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.numero").value(response.numero()))
                    .andExpect(jsonPath("$.status").value(response.status()));
        }
    }

    private OrdemServicoResponseDTO responseDTO() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        return new OrdemServicoResponseDTO(
                UUID.randomUUID(),
                "OS-000123",
                "EM_DIAGNOSTICO",
                "Barulho no motor",
                null,
                BigDecimal.ZERO,
                null,
                clienteId,
                "Cliente Teste",
                veiculoId,
                "ABC1B23"
        );
    }
}



