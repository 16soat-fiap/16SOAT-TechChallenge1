package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.config.SecurityConfig;
import com.autopecas.autopecas.dto.veiculo.VeiculoCreateDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoResponseDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoUpdateDTO;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.service.VeiculoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VeiculoController.class)
@Import(SecurityConfig.class)
@DisplayName("VeiculoController")
class VeiculoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VeiculoService veiculoService;

    @Nested
    @DisplayName("Testes dos endpoints")
    class EndpointTests {

        @Test
        @DisplayName("GET /api/veiculos deve listar veículos")
        void getApiVeiculosDeveListarVeiculos() throws Exception {
            // Given
            VeiculoResponseDTO response = responseDTO();
            when(veiculoService.listar()).thenReturn(List.of(response));

            // When / Then
            mockMvc.perform(get("/api/veiculos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].placa").value(response.placa()));
        }

        @Test
        @DisplayName("GET /api/veiculos/{id} deve mapear 404")
        void getApiVeiculosIdDeveMapear404() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            when(veiculoService.buscarPorId(id)).thenThrow(new ResourceNotFoundException("Veiculo não encontrado, id " + id));

            // When / Then
            mockMvc.perform(get("/api/veiculos/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Veiculo não encontrado, id " + id));
        }

        @Test
        @DisplayName("GET /api/veiculos/placa/{placa} deve buscar por placa")
        void getApiVeiculosPlacaPlacaDeveBuscarPorPlaca() throws Exception {
            // Given
            VeiculoResponseDTO response = responseDTO();
            when(veiculoService.buscarPorPlaca("ABC1B23")).thenReturn(response);

            // When / Then
            mockMvc.perform(get("/api/veiculos/placa/ABC1B23"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.placa").value(response.placa()));
        }

        @Test
        @DisplayName("GET /api/veiculos/cliente/{clienteId} deve listar por cliente")
        void getApiVeiculosClienteClienteIdDeveListarPorCliente() throws Exception {
            // Given
            VeiculoResponseDTO response = responseDTO();
            when(veiculoService.listarPorCliente(response.clienteId())).thenReturn(List.of(response));

            // When / Then
            mockMvc.perform(get("/api/veiculos/cliente/{clienteId}", response.clienteId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].clienteId").value(response.clienteId().toString()));
        }

        @Test
        @DisplayName("POST /api/veiculos deve criar veículo")
        void postApiVeiculosDeveCriarVeiculo() throws Exception {
            // Given
            VeiculoCreateDTO request = new VeiculoCreateDTO(UUID.randomUUID(), "ABC1B23", "1HGDM28153A000001", "12345678901", "GM", "Onix", 2024, "Branco");
            VeiculoResponseDTO response = responseDTO();
            when(veiculoService.criar(request)).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/veiculos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"clienteId\": \"%s\",
                                      \"placa\": \"ABC1B23\",
                                      \"chassi\": \"1HGDM28153A000001\",
                                      \"renavam\": \"12345678901\",
                                      \"marca\": \"GM\",
                                      \"modelo\": \"Onix\",
                                      \"anoModelo\": 2024,
                                      \"cor\": \"Branco\"
                                    }
                                    """.formatted(request.clienteId())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.placa").value(response.placa()));
        }

        @Test
        @DisplayName("POST /api/veiculos deve retornar 400 para payload inválido")
        void postApiVeiculosDeveRetornar400ParaPayloadInvalido() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/veiculos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.clienteId").value("Cliente é obrigatório"))
                    .andExpect(jsonPath("$.campos.placa").value("Placa é obrigatória"))
                    .andExpect(jsonPath("$.campos.marca").value("Marca é obrigatória"))
                    .andExpect(jsonPath("$.campos.modelo").value("Modelo é obrigatório"));
        }

        @Test
        @DisplayName("PUT /api/veiculos/{id} deve atualizar veículo")
        void putApiVeiculosIdDeveAtualizarVeiculo() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            VeiculoUpdateDTO request = new VeiculoUpdateDTO("Ford", "Ka", 2021, "Prata", null, null);
            VeiculoResponseDTO response = responseDTO();
            when(veiculoService.atualizar(id, request)).thenReturn(response);

            // When / Then
            mockMvc.perform(put("/api/veiculos/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"marca\": \"Ford\",
                                      \"modelo\": \"Ka\",
                                      \"anoModelo\": 2021,
                                      \"cor\": \"Prata\"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.marca").value(response.marca()));
        }

        @Test
        @DisplayName("DELETE /api/veiculos/{id} deve retornar 204")
        void deleteApiVeiculosIdDeveRetornar204() throws Exception {
            // Given
            UUID id = UUID.randomUUID();

            // When / Then
            mockMvc.perform(delete("/api/veiculos/{id}", id))
                    .andExpect(status().isNoContent());

            verify(veiculoService).deletar(id);
        }
    }

    private VeiculoResponseDTO responseDTO() {
        UUID clienteId = UUID.randomUUID();
        return new VeiculoResponseDTO(UUID.randomUUID(), "ABC1B23", "GM", "Onix", 2024, "Branco", true, clienteId);
    }
}



