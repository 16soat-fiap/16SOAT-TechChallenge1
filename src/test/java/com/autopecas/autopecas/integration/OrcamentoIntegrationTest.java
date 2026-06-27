package com.autopecas.autopecas.integration;

import com.autopecas.autopecas.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.dto.funcionario.MecanicoCreateDTO;
import com.autopecas.autopecas.dto.orcamento.AprovarRejeitarDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoCreateDTO;
import com.autopecas.autopecas.dto.os.AvancarStatusDTO;
import com.autopecas.autopecas.dto.os.OrdemServicoCreateDTO;
import com.autopecas.autopecas.dto.peca.PecaCreateDTO;
import com.autopecas.autopecas.dto.servico.ServicoCreateDTO;
import com.autopecas.autopecas.dto.veiculo.VeiculoCreateDTO;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para OrcamentoController -> OrcamentoService -> EstoqueService.
 *
 * Valida o fluxo completo do orçamento:
 *  1 Criar orçamento (RASCUNHO) com itens de serviço e peça
 *  2 Enviar (ENVIADA)
 *  3 Aprovar (APROVADA)  OS avança para EM_EXECUCAO, estoque é deduzido
 *  4 Rejeitar (CANCELADA) com motivo
 *  5 Segunda versão após rejeição versao=2
 *  6 Estoque insuficiente na aprovação  422
 *
 * Pré-condição obrigatória para aprovação:
 *   OS deve estar em AGUARDANDO_APROVACAO antes de criar e enviar o orçamento.
 */
@DisplayName("OrcamentoController - Integração")
class OrcamentoIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID clienteId;
    private UUID veiculoId;
    private UUID servicoId;
    private UUID pecaId;

    // ─── Setup ──────────────────────────────────────────────────────────────────

    @BeforeEach
    void configurarEntidades() throws Exception {
        // Cliente
        ClienteCreatePFDTO dtoCli = new ClienteCreatePFDTO(
                "Cliente Orcamento", "orc@teste.com", "11999990003",
                true, "52998224725",
                LocalDate.of(1990, 1, 1), null, null);
        String respCli = mockMvc.perform(post("/api/clientes/pf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoCli)))
                .andReturn().getResponse().getContentAsString();
        clienteId = UUID.fromString(objectMapper.readTree(respCli).get("id").asText());

        // Veículo
        VeiculoCreateDTO dtoVei = new VeiculoCreateDTO(
                clienteId, "BBB0002", null, null, "Chevrolet", "Onix", 2022, "Preto");
        String respVei = mockMvc.perform(post("/api/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoVei)))
                .andReturn().getResponse().getContentAsString();
        veiculoId = UUID.fromString(objectMapper.readTree(respVei).get("id").asText());

        // Serviço
        ServicoCreateDTO dtoSrv = new ServicoCreateDTO("Troca de Correia", "Troca correia dentada",
                new BigDecimal("300.00"), 120);
        String respSrv = mockMvc.perform(post("/api/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoSrv)))
                .andReturn().getResponse().getContentAsString();
        servicoId = UUID.fromString(objectMapper.readTree(respSrv).get("id").asText());

        // Peça com estoque = 10
        PecaCreateDTO dtoPec = new PecaCreateDTO(
                "PECA-ORC", "Correia Dentada", "Correia original",
                new BigDecimal("120.00"), 10, 2, "un");
        String respPec = mockMvc.perform(post("/api/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoPec)))
                .andReturn().getResponse().getContentAsString();
        pecaId = UUID.fromString(objectMapper.readTree(respPec).get("id").asText());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Cria uma OS e a avança até AGUARDANDO_APROVACAO.
     * Isso é pré-requisito para criar e aprovar orçamentos.
     */
    private UUID criarOSEmAguardandoAprovacao() throws Exception {
        // Cria OS
        OrdemServicoCreateDTO dtoOS = new OrdemServicoCreateDTO(
                clienteId, veiculoId, "Correia fazendo barulho", null, null);
        String respOS = mockMvc.perform(post("/api/ordens-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoOS)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID osId = UUID.fromString(objectMapper.readTree(respOS).get("id").asText());

        // RECEBIDA → EM_DIAGNOSTICO
        mockMvc.perform(patch("/api/ordens-servico/" + osId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvancarStatusDTO("EM_DIAGNOSTICO", null))))
                .andExpect(status().isOk());

        // EM_DIAGNOSTICO → AGUARDANDO_APROVACAO
        mockMvc.perform(patch("/api/ordens-servico/" + osId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvancarStatusDTO("AGUARDANDO_APROVACAO", null))))
                .andExpect(status().isOk());

        return osId;
    }

    private String urlOrcamentos(UUID osId) {
        return "/api/ordens-servico/" + osId + "/orcamentos";
    }

    private String criarOrcamento(UUID osId) throws Exception {
        OrcamentoCreateDTO dto = new OrcamentoCreateDTO(
                List.of(new OrcamentoCreateDTO.ItemServicoDTO(servicoId, 1)),
                List.of(new OrcamentoCreateDTO.ItemPecaDTO(pecaId, 2)),
                "Boleto 30 dias", 5,
                LocalDate.now().plusDays(7), null);

        return mockMvc.perform(post(urlOrcamentos(osId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    // ─── Criação ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/ordens-servico/{osId}/orcamentos")
    class CriacaoTests {

        @Test
        @DisplayName("deve criar orçamento em RASCUNHO com valores calculados")
        void deveCriarOrcamento() throws Exception {
            UUID osId = criarOSEmAguardandoAprovacao();

            mockMvc.perform(post(urlOrcamentos(osId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new OrcamentoCreateDTO(
                                    List.of(new OrcamentoCreateDTO.ItemServicoDTO(servicoId, 1)),
                                    List.of(new OrcamentoCreateDTO.ItemPecaDTO(pecaId, 1)),
                                    "À vista", 3,
                                    LocalDate.now().plusDays(5), "Obs de teste"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.status", is("RASCUNHO")))
                    .andExpect(jsonPath("$.versao", is(1)))
                    .andExpect(jsonPath("$.valorMaoObra").isNumber())
                    .andExpect(jsonPath("$.valorPecas").isNumber())
                    .andExpect(jsonPath("$.valorTotal").isNumber());
        }

        @Test
        @DisplayName("deve listar orçamentos da OS")
        void deveListarOrcamentos() throws Exception {
            UUID osId = criarOSEmAguardandoAprovacao();
            criarOrcamento(osId);

            mockMvc.perform(get(urlOrcamentos(osId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].versao", is(1)));
        }
    }

    // ─── Envio ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /{id}/enviar")
    class EnvioTests {

        @Test
        @DisplayName("deve enviar orçamento e mudar status para ENVIADA")
        void deveEnviarOrcamento() throws Exception {
            UUID osId = criarOSEmAguardandoAprovacao();
            String respOrc = criarOrcamento(osId);
            String orcId = objectMapper.readTree(respOrc).get("id").asText();

            mockMvc.perform(patch(urlOrcamentos(osId) + "/" + orcId + "/enviar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ENVIADA")));
        }
    }

    // ─── Aprovação (com impacto no estoque) ─────────────────────────────────────

    @Nested
    @DisplayName("PATCH /{id}/aprovar")
    class AprovacaoTests {

        @Test
        @DisplayName("deve aprovar orçamento, avançar OS para EM_EXECUCAO e decrementar estoque")
        void deveAprovarOrcamento() throws Exception {
            UUID osId = criarOSEmAguardandoAprovacao();
            String respOrc = criarOrcamento(osId);
            String orcId = objectMapper.readTree(respOrc).get("id").asText();

            // Envia
            mockMvc.perform(patch(urlOrcamentos(osId) + "/" + orcId + "/enviar"))
                    .andExpect(status().isOk());

            // Aprova
            mockMvc.perform(patch(urlOrcamentos(osId) + "/" + orcId + "/aprovar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("APROVADA")));

            // OS deve ter avançado para EM_EXECUCAO
            mockMvc.perform(get("/api/ordens-servico").param("status", "EM_EXECUCAO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

            // Estoque deve ter sido decrementado (10 - 2 = 8)
            mockMvc.perform(get("/api/pecas/" + pecaId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantidadeEstoque", is(8)));
        }

        @Test
        @DisplayName("deve retornar 422 ao aprovar orçamento com estoque insuficiente")
        void deveRetornar422QuandoEstoqueInsuficiente() throws Exception {
            // Cria peça sem estoque
            PecaCreateDTO dtoPecSemEstoque = new PecaCreateDTO(
                    "PECA-SEM", "Peça Sem Estoque", "Desc",
                    new BigDecimal("50.00"), 0, 1, "un");
            String respPecSem = mockMvc.perform(post("/api/pecas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoPecSemEstoque)))
                    .andReturn().getResponse().getContentAsString();
            UUID pecaSemEstoqueId = UUID.fromString(objectMapper.readTree(respPecSem).get("id").asText());

            UUID osId = criarOSEmAguardandoAprovacao();

            // Cria orçamento com a peça sem estoque
            OrcamentoCreateDTO dto = new OrcamentoCreateDTO(
                    null,
                    List.of(new OrcamentoCreateDTO.ItemPecaDTO(pecaSemEstoqueId, 5)),
                    null, 3,
                    LocalDate.now().plusDays(5), null);
            String respOrc = mockMvc.perform(post(urlOrcamentos(osId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            String orcId = objectMapper.readTree(respOrc).get("id").asText();

            // Envia
            mockMvc.perform(patch(urlOrcamentos(osId) + "/" + orcId + "/enviar"))
                    .andExpect(status().isOk());

            // Aprova → deve falhar por estoque insuficiente
            mockMvc.perform(patch(urlOrcamentos(osId) + "/" + orcId + "/aprovar"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status", is(422)));
        }
    }

    // ─── Rejeição ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /{id}/rejeitar")
    class RejeicaoTests {

        @Test
        @DisplayName("deve rejeitar orçamento e salvar o motivo")
        void deveRejeitarOrcamento() throws Exception {
            UUID osId = criarOSEmAguardandoAprovacao();
            String respOrc = criarOrcamento(osId);
            String orcId = objectMapper.readTree(respOrc).get("id").asText();

            mockMvc.perform(patch(urlOrcamentos(osId) + "/" + orcId + "/enviar"))
                    .andExpect(status().isOk());

            mockMvc.perform(patch(urlOrcamentos(osId) + "/" + orcId + "/rejeitar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AprovarRejeitarDTO("Valor fora do esperado"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELADA")));
        }

        @Test
        @DisplayName("deve criar segunda versão de orçamento após rejeição (versao=2)")
        void deveCriarSegundaVersaoAposRejeicao() throws Exception {
            UUID osId = criarOSEmAguardandoAprovacao();

            // Versão 1
            String respOrc1 = criarOrcamento(osId);
            String orc1Id = objectMapper.readTree(respOrc1).get("id").asText();
            mockMvc.perform(patch(urlOrcamentos(osId) + "/" + orc1Id + "/enviar"));
            mockMvc.perform(patch(urlOrcamentos(osId) + "/" + orc1Id + "/rejeitar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AprovarRejeitarDTO("Muito caro"))));

            // Versão 2
            String respOrc2 = criarOrcamento(osId);
            mockMvc.perform(post(urlOrcamentos(osId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new OrcamentoCreateDTO(
                                    List.of(new OrcamentoCreateDTO.ItemServicoDTO(servicoId, 1)),
                                    null, "Novo prazo", 3,
                                    LocalDate.now().plusDays(10), null))));

            // Lista orçamentos — deve ter pelo menos 2 versões
            mockMvc.perform(get(urlOrcamentos(osId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[1].versao", is(2)));
        }
    }
}
