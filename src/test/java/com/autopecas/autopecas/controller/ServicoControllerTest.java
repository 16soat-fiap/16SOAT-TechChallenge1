package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.config.SecurityConfig;
import com.autopecas.autopecas.dto.servico.ServicoCreateDTO;
import com.autopecas.autopecas.dto.servico.ServicoResponseDTO;
import com.autopecas.autopecas.service.ServicoService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServicoController.class)
@Import(SecurityConfig.class)
@DisplayName("ServicoController")
class ServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicoService servicoService;

    @Nested
    @DisplayName("Testes dos endpoints")
    class EndpointTests {

        @Test
        @DisplayName("GET /api/servicos deve listar serviços")
        void getApiServicosDeveListarServicos() throws Exception {
            // Given
            ServicoResponseDTO response = responseDTO();
            when(servicoService.listar()).thenReturn(List.of(response));

            // When / Then
            mockMvc.perform(get("/api/servicos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nome").value(response.nome()));
        }

        @Test
        @DisplayName("GET /api/servicos/{id} deve buscar por id")
        void getApiServicosIdDeveBuscarPorId() throws Exception {
            // Given
            ServicoResponseDTO response = responseDTO();
            when(servicoService.buscarPorId(response.id())).thenReturn(response);

            // When / Then
            mockMvc.perform(get("/api/servicos/{id}", response.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(response.id().toString()));
        }

        @Test
        @DisplayName("POST /api/servicos deve criar serviço")
        void postApiServicosDeveCriarServico() throws Exception {
            // Given
            ServicoResponseDTO response = responseDTO();
            when(servicoService.criar(any(ServicoCreateDTO.class))).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/servicos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nome": "Alinhamento",
                                      "descricao": "Completo",
                                      "precoBase": 199.90,
                                      "tempoEstimadoMinutos": 90
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome").value(response.nome()));
        }

        @Test
        @DisplayName("POST /api/servicos deve retornar 400 para payload inválido")
        void postApiServicosDeveRetornar400ParaPayloadInvalido() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/servicos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.campos.nome").exists())
                    .andExpect(jsonPath("$.campos.precoBase").exists());
        }

        @Test
        @DisplayName("POST /api/servicos deve retornar 400 quando preço base não é positivo")
        void postApiServicosDeveRetornar400ParaPrecoBaseNaoPositivo() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/servicos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nome": "Alinhamento",
                                      "descricao": "Completo",
                                      "precoBase": -10.00,
                                      "tempoEstimadoMinutos": 90
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.campos.precoBase").exists());
        }

        @Test
        @DisplayName("PUT /api/servicos/{id} deve atualizar serviço")
        void putApiServicosIdDeveAtualizarServico() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            ServicoResponseDTO response = responseDTO();
            when(servicoService.atualizar(eq(id), any(ServicoCreateDTO.class))).thenReturn(response);

            // When / Then
            mockMvc.perform(put("/api/servicos/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "nome": "Balanceamento",
                                      "descricao": "Rodas",
                                      "precoBase": 120.00,
                                      "tempoEstimadoMinutos": 45
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value(response.nome()));
        }

        @Test
        @DisplayName("DELETE /api/servicos/{id} deve retornar 204")
        void deleteApiServicosIdDeveRetornar204() throws Exception {
            // Given
            UUID id = UUID.randomUUID();

            // When / Then
            mockMvc.perform(delete("/api/servicos/{id}", id))
                    .andExpect(status().isNoContent());

            verify(servicoService).desativar(id);
        }
    }

    private ServicoResponseDTO responseDTO() {
        return new ServicoResponseDTO(UUID.randomUUID(), "Alinhamento", "Completo", new BigDecimal("199.90"), 90, true);
    }
}