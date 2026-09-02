package com.autopecas.autopecas.integration;

import com.autopecas.autopecas.adapter.in.web.dto.servico.ServicoCreateDTO;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para ServicoController → ServicoService → ServicoRepository.
 *
 * Objetivo: Validar o CRUD completo de Servico via HTTP, persistência real no
 * PostgreSQL e o contrato de respostas de erro.
 *
 * Cenários:
 *  - Criação com dados válidos → 201 + payload correto
 *  - Criação com nome em branco → 400 + mapa de erros de validação
 *  - Criação com preço nulo → 400 + mapa de erros de validação
 *  - Listagem → 200 + lista inclui serviço criado
 *  - Busca por ID inexistente → 404 + ErrorResponse com campos obrigatórios
 *  - Atualização de preço → 200 + valor persistido
 *  - Desativação → 204 + serviço não aparece na listagem
 */
@DisplayName("ServicoController - Integração")
class ServicoIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/servicos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private String criarServico(String nome, BigDecimal preco) throws Exception {
        ServicoCreateDTO dto = new ServicoCreateDTO(nome, "Descrição de teste", preco, 60);
        return mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    // ─── Criação ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/servicos")
    class CriacaoTests {

        @Test
        @DisplayName("deve criar serviço e retornar 201 com dados corretos")
        void deveCriarServico() throws Exception {
            ServicoCreateDTO dto = new ServicoCreateDTO("Troca de Óleo", "Serviço básico", new BigDecimal("150.00"), 60);

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.nome", is("Troca de Óleo")))
                    .andExpect(jsonPath("$.precoBase", is(150.00)))
                    .andExpect(jsonPath("$.ativo", is(true)));
        }

        @Test
        @DisplayName("deve retornar 400 e mapa de erros quando nome está em branco")
        void deveRetornar400QuandoNomeEmBranco() throws Exception {
            ServicoCreateDTO dto = new ServicoCreateDTO("", "Desc", new BigDecimal("100.00"), null);

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.campos.nome").exists());
        }

        @Test
        @DisplayName("deve retornar 400 quando preço base é nulo")
        void deveRetornar400QuandoPrecoBaseNulo() throws Exception {
            ServicoCreateDTO dto = new ServicoCreateDTO("Alinhamento", "Desc", null, null);

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.precoBase").exists());
        }
    }

    // ─── Consulta ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/servicos")
    class ConsultaTests {

        @Test
        @DisplayName("deve listar serviços e incluir o serviço recém-criado")
        void deveListarServicos() throws Exception {
            criarServico("Balanceamento", new BigDecimal("80.00"));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[*].nome", hasItem("Balanceamento")));
        }

        @Test
        @DisplayName("deve buscar serviço por ID e retornar 200")
        void deveBuscarServicoPorId() throws Exception {
            String resp = criarServico("Revisão Completa", new BigDecimal("500.00"));
            String id = objectMapper.readTree(resp).get("id").asText();

            mockMvc.perform(get(BASE + "/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome", is("Revisão Completa")));
        }

        @Test
        @DisplayName("deve retornar 404 com ErrorResponse quando ID não existe")
        void deveRetornar404AoBuscarIdInexistente() throws Exception {
            mockMvc.perform(get(BASE + "/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // ─── Atualização ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/servicos/{id}")
    class AtualizacaoTests {

        @Test
        @DisplayName("deve atualizar preço base e persistir o novo valor")
        void deveAtualizarPrecoBase() throws Exception {
            String resp = criarServico("Serviço Atualização", new BigDecimal("200.00"));
            String id = objectMapper.readTree(resp).get("id").asText();

            ServicoCreateDTO atualizar = new ServicoCreateDTO("Serviço Atualização", "Desc", new BigDecimal("350.00"), 90);
            mockMvc.perform(put(BASE + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(atualizar)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.precoBase", is(350.00)));

            // Confirma que o valor foi persistido ao buscar novamente
            mockMvc.perform(get(BASE + "/" + id))
                    .andExpect(jsonPath("$.precoBase", is(350.00)));
        }
    }

    // ─── Desativação ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/servicos/{id}")
    class DesativacaoTests {

        @Test
        @DisplayName("deve desativar serviço e ele não deve aparecer na listagem")
        void deveDesativarServico() throws Exception {
            String resp = criarServico("Serviço Para Desativar", new BigDecimal("50.00"));
            String id = objectMapper.readTree(resp).get("id").asText();

            mockMvc.perform(delete(BASE + "/" + id))
                    .andExpect(status().isNoContent());

            // Serviço desativado não deve aparecer na listagem
            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].nome", not(hasItem("Serviço Para Desativar"))));
        }
    }
}
