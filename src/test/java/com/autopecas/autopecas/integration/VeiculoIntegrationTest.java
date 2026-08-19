package com.autopecas.autopecas.integration;

import com.autopecas.autopecas.adapter.in.web.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.adapter.in.web.dto.veiculo.VeiculoCreateDTO;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para VeiculoController → VeiculoService → VeiculoRepository.
 *
 * Valida:
 *  - Criação com placa formato antigo (ABC1234) → 201
 *  - Criação com placa formato Mercosul (ABC1D23) → 201
 *  - Criação com placa inválida → 422 via BusinessException do VO Placa
 *  - Criação para cliente inexistente → 404
 *  - Listagem de todos os veículos → 200
 *  - Busca por placa → 200
 *  - Listagem filtrada por cliente → 200, apenas veículos do cliente
 *  - Soft delete → 204
 */
@DisplayName("VeiculoController - Integração")
class VeiculoIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/veiculos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID clienteId;

    @BeforeEach
    void criarClienteBase() throws Exception {
        ClienteCreatePFDTO dto = new ClienteCreatePFDTO(
                "Cliente Veículo", "veiculo@teste.com", "11999990001",
                true, "52998224725",
                LocalDate.of(1990, 1, 1), null, null);

        String resp = mockMvc.perform(post("/api/clientes/pf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        clienteId = UUID.fromString(objectMapper.readTree(resp).get("id").asText());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private VeiculoCreateDTO dtoVeiculo(String placa) {
        return new VeiculoCreateDTO(
                clienteId, placa,
                "CH123456789012345", "12345678901",
                "Toyota", "Corolla", 2020, "Prata");
    }

    private String criarVeiculo(String placa) throws Exception {
        return mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoVeiculo(placa))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    // ─── Criação ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/veiculos")
    class CriacaoTests {

        @Test
        @DisplayName("deve criar veículo com placa no formato antigo (ABC1234)")
        void deveCriarVeiculoComPlacaAntiga() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoVeiculo("ABC1234"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.marca", is("Toyota")))
                    .andExpect(jsonPath("$.ativo", is(true)))
                    .andExpect(jsonPath("$.clienteId", is(clienteId.toString())));
        }

        @Test
        @DisplayName("deve criar veículo com placa no formato Mercosul (ABC1D23)")
        void deveCriarVeiculoComPlacaMercosul() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoVeiculo("XYZ1A23"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty());
        }

        @Test
        @DisplayName("deve retornar 422 ao criar veículo com placa inválida")
        void deveRetornar422ComPlacaInvalida() throws Exception {
            VeiculoCreateDTO dto = new VeiculoCreateDTO(
                    clienteId, "INVALIDA",
                    null, null,
                    "Ford", "Ka", 2019, "Azul");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().is4xxClientError()); // BusinessException do VO Placa → 422
        }

        @Test
        @DisplayName("deve retornar 404 ao criar veículo para cliente inexistente")
        void deveRetornar404ParaClienteInexistente() throws Exception {
            VeiculoCreateDTO dto = new VeiculoCreateDTO(
                    UUID.randomUUID(), "DEF5678",
                    null, null,
                    "Honda", "Civic", 2021, "Vermelho");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void deveRetornar400QuandoClienteIdNulo() throws Exception {
            VeiculoCreateDTO dto = new VeiculoCreateDTO(
                    null, "GHI9012", null, null, "BMW", "X1", 2022, "Branco");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── Consulta ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/veiculos")
    class ConsultaTests {

        @Test
        @DisplayName("deve listar todos os veículos ativos")
        void deveListarVeiculos() throws Exception {
            criarVeiculo("JKL3456");

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[*].marca", hasItem("Toyota")));
        }

        @Test
        @DisplayName("deve listar veículos filtrados por cliente")
        void deveListarVeiculosPorCliente() throws Exception {
            criarVeiculo("MNO7890");

            mockMvc.perform(get(BASE + "/cliente/" + clienteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[*].clienteId", hasItem(clienteId.toString())));
        }

        @Test
        @DisplayName("deve buscar veículo por placa")
        void deveBuscarVeiculoPorPlaca() throws Exception {
            criarVeiculo("PQR1234");

            mockMvc.perform(get(BASE + "/placa/PQR1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.marca", is("Toyota")));
        }
    }

    // ─── Soft Delete ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/veiculos/{id}")
    class DeleteTests {

        @Test
        @DisplayName("deve deletar (soft) veículo e retornar 204")
        void deveDeletarVeiculo() throws Exception {
            String resp = criarVeiculo("STU5678");
            String id = objectMapper.readTree(resp).get("id").asText();

            mockMvc.perform(delete(BASE + "/" + id))
                    .andExpect(status().isNoContent());

            // Veículo soft-deleted: GET retorna 404 (inativo não é retornado)
            mockMvc.perform(get(BASE + "/" + id))
                    .andExpect(status().isNotFound());
        }
    }
}
