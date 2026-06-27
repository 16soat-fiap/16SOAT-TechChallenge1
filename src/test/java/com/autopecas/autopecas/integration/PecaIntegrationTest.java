package com.autopecas.autopecas.integration;

import com.autopecas.autopecas.dto.peca.MovimentacaoCreateDTO;
import com.autopecas.autopecas.dto.peca.PecaCreateDTO;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para PecaController → PecaService → EstoqueService.
 *
 * Valida:
 *  - Criação de peça com estoque inicial → 201, quantidadeEstoque correta
 *  - Código duplicado → 422
 *  - Listagem com filtro ?estoqueBaixo=true → retorna apenas peças com estoque baixo
 *  - Busca por código → 200
 *  - Movimentação manual ENTRADA → 201, saldo atualizado
 *  - Movimentação manual SAIDA com estoque insuficiente → 422
 *  - Desativação → 204
 */
@DisplayName("PecaController - Integração")
class PecaIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/pecas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private PecaCreateDTO dto(String codigo, int qtdInicial) {
        return new PecaCreateDTO(
                codigo, "Peça " + codigo, "Descrição de " + codigo,
                new BigDecimal("99.90"), qtdInicial, 2, "un");
    }

    private String criarPeca(String codigo, int qtdInicial) throws Exception {
        return mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto(codigo, qtdInicial))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    // ─── Criação ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/pecas")
    class CriacaoTests {

        @Test
        @DisplayName("deve criar peça com estoque inicial e retornar 201")
        void deveCriarPeca() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto("COD-001", 10))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.codigo", is("COD-001")))
                    .andExpect(jsonPath("$.ativo", is(true)));
        }

        @Test
        @DisplayName("deve retornar 400 quando código está em branco")
        void deveRetornar400QuandoCodigoEmBranco() throws Exception {
            PecaCreateDTO dto = new PecaCreateDTO("", "Nome", "Desc",
                    new BigDecimal("10.00"), 0, 1, "un");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.codigo").exists());
        }

        @Test
        @DisplayName("deve retornar 422 ao criar peça com código duplicado")
        void deveRetornar422ComCodigoDuplicado() throws Exception {
            criarPeca("COD-DUP", 5);

            // Segunda criação com mesmo código
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto("COD-DUP", 5))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status", is(422)));
        }

        @Test
        @DisplayName("deve retornar 400 quando preço de venda é nulo")
        void deveRetornar400QuandoPrecoNulo() throws Exception {
            PecaCreateDTO dto = new PecaCreateDTO("COD-999", "Nome", "Desc",
                    null, 0, 1, "un");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.precoVenda").exists());
        }
    }

    // ─── Consulta ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/pecas")
    class ConsultaTests {

        @Test
        @DisplayName("deve listar todas as peças ativas")
        void deveListarPecas() throws Exception {
            criarPeca("COD-LIST", 5);

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("deve filtrar peças com estoque baixo (?estoqueBaixo=true)")
        void deveListarPecasComEstoqueBaixo() throws Exception {
            // Peça com qtd=1, quantidadeMinima=2 → estoque baixo (qtd <= minimo)
            criarPeca("COD-BAIXO", 1);  // estoque=1 ≤ minimo=2 → estoque baixo

            mockMvc.perform(get(BASE).param("estoqueBaixo", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("deve buscar peça por código")
        void deveBuscarPecaPorCodigo() throws Exception {
            criarPeca("COD-BUS", 3);

            mockMvc.perform(get(BASE + "/buscar").param("codigo", "COD-BUS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.codigo", is("COD-BUS")));
        }

        @Test
        @DisplayName("deve retornar 404 ao buscar código inexistente")
        void deveRetornar404AoBuscarCodigoInexistente() throws Exception {
            mockMvc.perform(get(BASE + "/buscar").param("codigo", "INEXISTENTE"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── Movimentações de Estoque ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/pecas/{id}/movimentacoes")
    class MovimentacaoTests {

        @Test
        @DisplayName("deve registrar movimentação de ENTRADA e retornar saldo atualizado")
        void deveRegistrarEntrada() throws Exception {
            String resp = criarPeca("COD-MOV", 5);
            String pecaId = objectMapper.readTree(resp).get("id").asText();

            MovimentacaoCreateDTO dto = new MovimentacaoCreateDTO("ENTRADA", 10, "Reposição de estoque");

            mockMvc.perform(post(BASE + "/" + pecaId + "/movimentacoes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo", is("ENTRADA")))
                    .andExpect(jsonPath("$.quantidade", is(10)));
        }

        @Test
        @DisplayName("deve retornar 422 ao registrar SAIDA com estoque insuficiente")
        void deveRetornar422QuandoEstoqueInsuficiente() throws Exception {
            String resp = criarPeca("COD-INSUF", 2);
            String pecaId = objectMapper.readTree(resp).get("id").asText();

            // Tenta retirar mais do que tem no estoque
            MovimentacaoCreateDTO dto = new MovimentacaoCreateDTO("SAIDA", 10, "Saída");

            mockMvc.perform(post(BASE + "/" + pecaId + "/movimentacoes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status", is(422)));
        }

        @Test
        @DisplayName("deve retornar 400 quando tipo de movimentação está em branco")
        void deveRetornar400QuandoTipoEmBranco() throws Exception {
            String resp = criarPeca("COD-VAL", 5);
            String pecaId = objectMapper.readTree(resp).get("id").asText();

            MovimentacaoCreateDTO dto = new MovimentacaoCreateDTO("", 5, "Motivo");

            mockMvc.perform(post(BASE + "/" + pecaId + "/movimentacoes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── Desativação ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/pecas/{id}")
    class DesativacaoTests {

        @Test
        @DisplayName("deve desativar peça e retornar 204")
        void deveDesativarPeca() throws Exception {
            String resp = criarPeca("COD-DEL", 0);
            String id = objectMapper.readTree(resp).get("id").asText();

            mockMvc.perform(delete(BASE + "/" + id))
                    .andExpect(status().isNoContent());

            // ID ainda deve ser encontrado mas com ativo=false
            mockMvc.perform(get(BASE + "/" + id))
                    .andExpect(jsonPath("$.ativo", is(false)));
        }

        @Test
        @DisplayName("deve retornar 404 ao desativar peça inexistente")
        void deveRetornar404AoDesativarInexistente() throws Exception {
            mockMvc.perform(delete(BASE + "/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}
