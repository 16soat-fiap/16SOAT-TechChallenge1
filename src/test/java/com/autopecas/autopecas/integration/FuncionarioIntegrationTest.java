package com.autopecas.autopecas.integration;

import com.autopecas.autopecas.dto.funcionario.AtendenteCreateDTO;
import com.autopecas.autopecas.dto.funcionario.MecanicoCreateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para FuncionarioController -> FuncionarioService -> FuncionarioRepository.
 *
 * Valida:
 *  - Criação de Mecânico  201, matrícula no formato MEC-XXXX (sequence PostgreSQL)
 *  - Criação de Atendente  201, matrícula no formato ATD-XXXX
 *  - CPF duplicado entre funcionários  422
 *  - Campos obrigatórios ausentes  400 com mapa de erros
 *  - Listagem  200
 *  - Busca por ID  200 / 404
 *  - Desativação  204
 */
@DisplayName("FuncionarioController - Integração")
class FuncionarioIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/funcionarios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private MecanicoCreateDTO dtoMecanico(String cpf) {
        return new MecanicoCreateDTO(
                "Carlos Mecânico", cpf,
                "carlos@oficina.com", "11977770001",
                LocalDate.of(1985, 3, 20));
    }

    private AtendenteCreateDTO dtoAtendente(String cpf) {
        return new AtendenteCreateDTO(
                "Ana Atendente", cpf,
                "ana@oficina.com", "11977770002",
                LocalDate.of(1992, 7, 10));
    }

    private String criarMecanico(String cpf) throws Exception {
        return mockMvc.perform(post(BASE + "/mecanico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoMecanico(cpf))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    // ─── Criação Mecânico ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/funcionarios/mecanico")
    class CriacaoMecanicoTests {

        @Test
        @DisplayName("deve criar mecânico com matrícula no formato MEC-XXXX")
        void deveCriarMecanico() throws Exception {
            mockMvc.perform(post(BASE + "/mecanico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoMecanico("11111111111"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.nome", is("Carlos Mecânico")))
                    .andExpect(jsonPath("$.matricula", matchesPattern("MEC-\\d{4}")))
                    .andExpect(jsonPath("$.tipo", is("MECANICO")))
                    .andExpect(jsonPath("$.ativo", is(true)));
        }

        @Test
        @DisplayName("deve retornar 400 quando nome está em branco")
        void deveRetornar400QuandoNomeEmBranco() throws Exception {
            MecanicoCreateDTO dto = new MecanicoCreateDTO("", "22222222222", null, null, null);

            mockMvc.perform(post(BASE + "/mecanico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.nome").exists());
        }

        @Test
        @DisplayName("deve retornar 422 ao criar mecânico com CPF já cadastrado")
        void deveRetornar422ComCpfDuplicado() throws Exception {
            criarMecanico("33333333333");

            // Segunda tentativa com mesmo CPF
            mockMvc.perform(post(BASE + "/mecanico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoMecanico("33333333333"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status", is(422)));
        }
    }

    // ─── Criação Atendente ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/funcionarios/atendente")
    class CriacaoAtendenteTests {

        @Test
        @DisplayName("deve criar atendente com matrícula no formato ATD-XXXX")
        void deveCriarAtendente() throws Exception {
            mockMvc.perform(post(BASE + "/atendente")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoAtendente("44444444444"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.matricula", matchesPattern("ATD-\\d{4}")))
                    .andExpect(jsonPath("$.tipo", is("ATENDENTE")));
        }
    }

    // ─── Consulta ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/funcionarios")
    class ConsultaTests {

        @Test
        @DisplayName("deve listar funcionários ativos")
        void deveListarFuncionarios() throws Exception {
            criarMecanico("55555555555");

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("deve buscar funcionário por ID")
        void deveBuscarFuncionarioPorId() throws Exception {
            String resp = criarMecanico("66666666666");
            String id = objectMapper.readTree(resp).get("id").asText();

            mockMvc.perform(get(BASE + "/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(id)));
        }

        @Test
        @DisplayName("deve retornar 404 ao buscar ID inexistente")
        void deveRetornar404AoBuscarIdInexistente() throws Exception {
            mockMvc.perform(get(BASE + "/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }
    }

    // ─── Desativação ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/funcionarios/{id}")
    class DesativacaoTests {

        @Test
        @DisplayName("deve desativar funcionário e retornar 204")
        void deveDesativarFuncionario() throws Exception {
            String resp = criarMecanico("77777777777");
            String id = objectMapper.readTree(resp).get("id").asText();

            mockMvc.perform(delete(BASE + "/" + id))
                    .andExpect(status().isNoContent());

            // Funcionário desativado não deve aparecer na busca por ID (ativo=false)
            mockMvc.perform(get(BASE + "/" + id))
                    .andExpect(jsonPath("$.ativo", is(false)));
        }
    }
}
