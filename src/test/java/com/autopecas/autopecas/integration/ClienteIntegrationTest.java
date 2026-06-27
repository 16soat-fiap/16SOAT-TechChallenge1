package com.autopecas.autopecas.integration;

import com.autopecas.autopecas.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.dto.cliente.ClienteCreatePJDTO;
import com.autopecas.autopecas.dto.cliente.ClienteUpdateDTO;
import tools.jackson.databind.ObjectMapper;
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
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para ClienteController -> ClienteService -> ClienteRepository.
 *
 * Valida:
 *  - Criação PF com CPF válido 201, documento retornado no campo correto
 *  - Criação PJ com CNPJ válido 201
 *  - CPF duplicado  422 com mensagem de negócio
 *  - CNPJ inválido (formato errado)  422 ou 500 via BusinessException do VO
 *  - Busca por documento (CPF / CNPJ) 200
 *  - Busca por documento inexistente  404
 *  - Busca por ID inexistente  404
 *  - Atualização  200 com dados persistidos
 *  - Desativação  204, cliente desaparece da listagem
 */
@DisplayName("ClienteController - Integração")
class ClienteIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/clientes";

    // CPF e CNPJ válidos para testes
    private static final String CPF_VALIDO     = "52998224725";
    private static final String CNPJ_VALIDO    = "12345678000195";
    private static final String CPF_ALTERNATIVO = "11144477735";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private ClienteCreatePFDTO dtoPF(String cpf) {
        return new ClienteCreatePFDTO(
                "João Silva", "joao@teste.com", "11999990000",
                true, cpf,
                LocalDate.of(1990, 5, 15), "RG-12345", "MASCULINO");
    }

    private ClienteCreatePJDTO dtoPJ(String cnpj) {
        return new ClienteCreatePJDTO(
                "Empresa Teste", "empresa@teste.com", "1133334444",
                false, cnpj, "Empresa Teste LTDA", "ISENTA");
    }

    private String criarClientePF(String cpf) throws Exception {
        return mockMvc.perform(post(BASE + "/pf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoPF(cpf))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String criarClientePJ(String cnpj) throws Exception {
        return mockMvc.perform(post(BASE + "/pj")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoPJ(cnpj))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    // ─── Criação PF ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/clientes/pf")
    class CriacaoPFTests {

        @Test
        @DisplayName("deve criar cliente PF com CPF válido e retornar 201")
        void deveCriarClientePF() throws Exception {
            mockMvc.perform(post(BASE + "/pf")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoPF(CPF_VALIDO))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.nome", is("João Silva")))
                    .andExpect(jsonPath("$.email", is("joao@teste.com")))
                    .andExpect(jsonPath("$.documento").isNotEmpty());
        }

        @Test
        @DisplayName("deve retornar 422 ao criar cliente PF com CPF duplicado")
        void deveRetornar422ComCpfDuplicado() throws Exception {
            criarClientePF(CPF_VALIDO);

            // Segunda criação com mesmo CPF
            mockMvc.perform(post(BASE + "/pf")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoPF(CPF_VALIDO))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status", is(422)))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("deve retornar 400 quando nome está em branco")
        void deveRetornar400QuandoNomeEmBranco() throws Exception {
            ClienteCreatePFDTO dto = new ClienteCreatePFDTO(
                    "", "email@teste.com", null, true,
                    CPF_VALIDO, null, null, null);

            mockMvc.perform(post(BASE + "/pf")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.nome").exists());
        }

        @Test
        @DisplayName("deve retornar 400 quando CPF está em branco")
        void deveRetornar400QuandoCpfEmBranco() throws Exception {
            ClienteCreatePFDTO dto = new ClienteCreatePFDTO(
                    "Nome Válido", null, null, true,
                    "", null, null, null);

            mockMvc.perform(post(BASE + "/pf")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.cpf").exists());
        }
    }

    // ─── Criação PJ ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/clientes/pj")
    class CriacaoPJTests {

        @Test
        @DisplayName("deve criar cliente PJ com CNPJ válido e retornar 201")
        void deveCriarClientePJ() throws Exception {
            mockMvc.perform(post(BASE + "/pj")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoPJ(CNPJ_VALIDO))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.nome", is("Empresa Teste")))
                    .andExpect(jsonPath("$.documento").isNotEmpty());
        }

        @Test
        @DisplayName("deve retornar 422 ao criar cliente PJ com CNPJ duplicado")
        void deveRetornar422ComCnpjDuplicado() throws Exception {
            criarClientePJ(CNPJ_VALIDO);

            mockMvc.perform(post(BASE + "/pj")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoPJ(CNPJ_VALIDO))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status", is(422)));
        }

        @Test
        @DisplayName("deve retornar 400 quando razão social está em branco")
        void deveRetornar400QuandoRazaoSocialEmBranco() throws Exception {
            ClienteCreatePJDTO dto = new ClienteCreatePJDTO(
                    "Empresa", null, null, false, CNPJ_VALIDO, "", null);

            mockMvc.perform(post(BASE + "/pj")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.razaoSocial").exists());
        }
    }

    // ─── Consulta ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clientes")
    class ConsultaTests {

        @Test
        @DisplayName("deve listar clientes ativos incluindo o recém-criado")
        void deveListarClientes() throws Exception {
            criarClientePF(CPF_VALIDO);

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[*].nome", hasItem("João Silva")));
        }

        @Test
        @DisplayName("deve buscar cliente por CPF via /buscarDOC")
        void deveBuscarClientePorCpf() throws Exception {
            criarClientePF(CPF_VALIDO);

            mockMvc.perform(get(BASE + "/buscarDOC").param("documento", CPF_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome", is("João Silva")));
        }

        @Test
        @DisplayName("deve buscar cliente por CNPJ via /buscarDOC")
        void deveBuscarClientePorCnpj() throws Exception {
            criarClientePJ(CNPJ_VALIDO);

            mockMvc.perform(get(BASE + "/buscarDOC").param("documento", CNPJ_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome", is("Empresa Teste")));
        }

        @Test
        @DisplayName("deve retornar 404 ao buscar por documento inexistente")
        void deveRetornar404AoBuscarDocumentoInexistente() throws Exception {
            mockMvc.perform(get(BASE + "/buscarDOC").param("documento", CPF_ALTERNATIVO))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }

        @Test
        @DisplayName("deve retornar 404 ao buscar por ID inexistente")
        void deveRetornar404AoBuscarIdInexistente() throws Exception {
            mockMvc.perform(get(BASE + "/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }
    }

    // ─── Atualização ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/clientes/{id}")
    class AtualizacaoTests {

        @Test
        @DisplayName("deve atualizar nome e email e persistir os novos valores")
        void deveAtualizarCliente() throws Exception {
            String resp = criarClientePF(CPF_VALIDO);
            String id = objectMapper.readTree(resp).get("id").asText();

            ClienteUpdateDTO update = new ClienteUpdateDTO("Nome Atualizado", "novo@email.com", "11888880000", true);
            mockMvc.perform(put(BASE + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome", is("Nome Atualizado")))
                    .andExpect(jsonPath("$.email", is("novo@email.com")));

            // Confirma persistência
            mockMvc.perform(get(BASE + "/" + id))
                    .andExpect(jsonPath("$.nome", is("Nome Atualizado")));
        }
    }

    // ─── Desativação ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/clientes/{id}")
    class DesativacaoTests {

        @Test
        @DisplayName("deve desativar cliente e ele não deve aparecer na listagem")
        void deveDesativarCliente() throws Exception {
            String resp = criarClientePF(CPF_VALIDO);
            String id = objectMapper.readTree(resp).get("id").asText();

            mockMvc.perform(delete(BASE + "/" + id))
                    .andExpect(status().isNoContent());

            // Cliente desativado não deve aparecer na listagem
            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", not(hasItem(id))));
        }
    }
}
