package com.autopecas.autopecas.integration;

import com.autopecas.autopecas.adapter.in.web.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.adapter.in.web.dto.funcionario.AtendenteCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.funcionario.MecanicoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.os.AtribuirMecanicoDTO;
import com.autopecas.autopecas.adapter.in.web.dto.os.AvancarStatusDTO;
import com.autopecas.autopecas.adapter.in.web.dto.os.DiagnosticoDTO;
import com.autopecas.autopecas.adapter.in.web.dto.os.OrdemServicoCreateDTO;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para OrdemServicoController → OrdemServicoService.
 *
 * Valida o ciclo de vida completo de uma OS:
 *  RECEBIDA -> EM_DIAGNOSTICO -> [diagnóstico registrado] -> AGUARDANDO_APROVACAO
 *
 * Também valida:
 *  - Geração do número no formato OS-XXXXXX (sequence PostgreSQL)
 *  - Transição inválida  422
 *  - Busca por número  200
 *  - Listagem paginada com filtro de status  200
 *  - Atribuição de mecânico  200
 *  - Validações de campos obrigatórios  400
 */
@DisplayName("OrdemServicoController - Integração")
class OrdemServicoIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/ordens-servico";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID clienteId;
    private UUID veiculoId;
    private UUID mecanicoId;

    // ─── Setup comum ────────────────────────────────────────────────────────────

    @BeforeEach
    void configurarEntidades() throws Exception {
        // Cria cliente
        ClienteCreatePFDTO dtoCli = new ClienteCreatePFDTO(
                "Cliente OS", "os@teste.com", "11999990002",
                true, "52998224725",
                LocalDate.of(1988, 6, 15), null, null);
        String respCli = mockMvc.perform(post("/api/clientes/pf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoCli)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        clienteId = UUID.fromString(objectMapper.readTree(respCli).get("id").asText());

        // Cria veículo
        VeiculoCreateDTO dtoVei = new VeiculoCreateDTO(
                clienteId, "AAA0001", null, null, "Fiat", "Uno", 2018, "Branco");
        String respVei = mockMvc.perform(post("/api/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoVei)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        veiculoId = UUID.fromString(objectMapper.readTree(respVei).get("id").asText());

        // Cria mecânico
        MecanicoCreateDTO dtoMec = new MecanicoCreateDTO(
                "Mecânico OS", "11111111110", "mec@oficina.com", null, null);
        String respMec = mockMvc.perform(post("/api/funcionarios/mecanico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoMec)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mecanicoId = UUID.fromString(objectMapper.readTree(respMec).get("id").asText());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private String criarOS() throws Exception {
        OrdemServicoCreateDTO dto = new OrdemServicoCreateDTO(
                clienteId, veiculoId,
                "Barulho no motor ao acelerar",
                "Veículo entrou pela manhã", 45000);

        return mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private void avancarStatus(String osId, String novoStatus) throws Exception {
        AvancarStatusDTO dto = new AvancarStatusDTO(novoStatus, "Avanço por teste");
        mockMvc.perform(patch(BASE + "/" + osId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    // ─── Criação ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/ordens-servico")
    class CriacaoTests {

        @Test
        @DisplayName("deve criar OS e retornar 201 com número no formato OS-XXXXXX")
        void deveCriarOS() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new OrdemServicoCreateDTO(
                                    clienteId, veiculoId, "Troca de freios", null, null))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.numero", matchesPattern("OS-\\d{6}")))
                    .andExpect(jsonPath("$.status", is("RECEBIDA")))
                    .andExpect(jsonPath("$.clienteId", is(clienteId.toString())))
                    .andExpect(jsonPath("$.veiculoId", is(veiculoId.toString())));
        }

        @Test
        @DisplayName("deve retornar 400 quando queixaCliente está em branco")
        void deveRetornar400QuandoQueixaEmBranco() throws Exception {
            OrdemServicoCreateDTO dto = new OrdemServicoCreateDTO(
                    clienteId, veiculoId, "", null, null);

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.queixaCliente").exists());
        }

        @Test
        @DisplayName("deve retornar 404 quando cliente não existe")
        void deveRetornar404QuandoClienteInexistente() throws Exception {
            OrdemServicoCreateDTO dto = new OrdemServicoCreateDTO(
                    UUID.randomUUID(), veiculoId, "Queixa válida", null, null);

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── Avanço de Status ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/ordens-servico/{id}/status")
    class AvancoStatusTests {

        @Test
        @DisplayName("deve avançar OS de RECEBIDA para EM_DIAGNOSTICO")
        void deveAvancarParaEmDiagnostico() throws Exception {
            String resp = criarOS();
            String id = objectMapper.readTree(resp).get("id").asText();

            AvancarStatusDTO dto = new AvancarStatusDTO("EM_DIAGNOSTICO", "Início do diagnóstico");

            mockMvc.perform(patch(BASE + "/" + id + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("EM_DIAGNOSTICO")));
        }

        @Test
        @DisplayName("deve retornar 422 em transição de status inválida (RECEBIDA → ENTREGUE)")
        void deveRetornar422EmTransicaoInvalida() throws Exception {
            String resp = criarOS();
            String id = objectMapper.readTree(resp).get("id").asText();

            AvancarStatusDTO dto = new AvancarStatusDTO("ENTREGUE", "Transição inválida");

            mockMvc.perform(patch(BASE + "/" + id + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status", is(422)));
        }

        @Test
        @DisplayName("deve retornar 422 com status desconhecido")
        void deveRetornar422ComStatusDesconhecido() throws Exception {
            String resp = criarOS();
            String id = objectMapper.readTree(resp).get("id").asText();

            AvancarStatusDTO dto = new AvancarStatusDTO("STATUS_INVALIDO", null);

            mockMvc.perform(patch(BASE + "/" + id + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("deve percorrer RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO")
        void devePercorrerFluxoParcial() throws Exception {
            String resp = criarOS();
            String id = objectMapper.readTree(resp).get("id").asText();

            avancarStatus(id, "EM_DIAGNOSTICO");
            avancarStatus(id, "AGUARDANDO_APROVACAO");

            mockMvc.perform(get(BASE + "/" + objectMapper.readTree(resp).get("numero").asText()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("AGUARDANDO_APROVACAO")));
        }
    }

    // ─── Diagnóstico ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/ordens-servico/{id}/diagnostico")
    class DiagnosticoTests {

        @Test
        @DisplayName("deve registrar diagnóstico quando OS está em EM_DIAGNOSTICO")
        void deveRegistrarDiagnostico() throws Exception {
            String resp = criarOS();
            String id = objectMapper.readTree(resp).get("id").asText();
            avancarStatus(id, "EM_DIAGNOSTICO");

            DiagnosticoDTO dto = new DiagnosticoDTO("Correia dentada desgastada. Necessita troca urgente.");

            mockMvc.perform(patch(BASE + "/" + id + "/diagnostico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.diagnostico", is("Correia dentada desgastada. Necessita troca urgente.")));
        }

        @Test
        @DisplayName("deve retornar 422 ao registrar diagnóstico em OS que não está em EM_DIAGNOSTICO")
        void deveRetornar422QuandoOSNaoEstaEmDiagnostico() throws Exception {
            String resp = criarOS();
            String id = objectMapper.readTree(resp).get("id").asText();
            // OS está em RECEBIDA, não em EM_DIAGNOSTICO

            DiagnosticoDTO dto = new DiagnosticoDTO("Diagnóstico prematuro");

            mockMvc.perform(patch(BASE + "/" + id + "/diagnostico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status", is(422)));
        }
    }

    // ─── Atribuição de Mecânico ──────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/ordens-servico/{id}/mecanico")
    class AtribuicaoMecanicoTests {

        @Test
        @DisplayName("deve atribuir mecânico à OS")
        void deveAtribuirMecanico() throws Exception {
            String resp = criarOS();
            String id = objectMapper.readTree(resp).get("id").asText();

            AtribuirMecanicoDTO dto = new AtribuirMecanicoDTO(mecanicoId);
            mockMvc.perform(patch(BASE + "/" + id + "/mecanico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 422 ao atribuir funcionário que não é mecânico")
        void deveRetornar422QuandoFuncionarioNaoEhMecanico() throws Exception {
            // Cria um atendente
            AtendenteCreateDTO dtoAte = new AtendenteCreateDTO(
                    "Atendente OS", "22222222220", "ate@oficina.com", null, null);
            String respAte = mockMvc.perform(post("/api/funcionarios/atendente")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoAte)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            UUID atendenteId = UUID.fromString(objectMapper.readTree(respAte).get("id").asText());

            String resp = criarOS();
            String id = objectMapper.readTree(resp).get("id").asText();

            AtribuirMecanicoDTO dto = new AtribuirMecanicoDTO(atendenteId);
            mockMvc.perform(patch(BASE + "/" + id + "/mecanico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status", is(422)));
        }
    }

    // ─── Consulta ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/ordens-servico")
    class ConsultaTests {

        @Test
        @DisplayName("deve buscar OS por número e retornar 200")
        void deveBuscarOSPorNumero() throws Exception {
            String resp = criarOS();
            String numero = objectMapper.readTree(resp).get("numero").asText();

            mockMvc.perform(get(BASE + "/" + numero))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.numero", is(numero)))
                    .andExpect(jsonPath("$.status", is("RECEBIDA")));
        }

        @Test
        @DisplayName("deve retornar 404 ao buscar número inexistente")
        void deveRetornar404AoBuscarNumeroInexistente() throws Exception {
            mockMvc.perform(get(BASE + "/OS-999999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve listar OS paginado com filtro de status")
        void deveListarOSComFiltroDeStatus() throws Exception {
            criarOS();

            mockMvc.perform(get(BASE).param("status", "RECEBIDA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable").exists());
        }

        @Test
        @DisplayName("deve listar OS sem filtro retornando estrutura paginada")
        void deveListarOSSemFiltro() throws Exception {
            criarOS();

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").isNumber());
        }
    }
}
