package com.autopecas.autopecas.integration;

import com.autopecas.autopecas.config.SecurityTestConfig;
import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.ClientePF;
import com.autopecas.autopecas.domain.valueobject.CPF;
import com.autopecas.autopecas.repository.ClienteRepository;
import com.autopecas.autopecas.util.test.JwtTestUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(SecurityTestConfig.class)
@AutoConfigureJsonTesters
@DisplayName("ClienteController — Testes de Integração com Testcontainers")
class ClienteControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("autopecas_test")
                    .withUsername("autopecas")
                    .withPassword("autopecas")
                    .withInitScript("init-test.sql")
                    .withReuse(true);

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",                 POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",            POSTGRES::getUsername);
        registry.add("spring.datasource.password",            POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name",  () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto",        () -> "create-drop");
        registry.add("spring.jpa.database-platform",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled",                 () -> "false");
        registry.add("spring.main.allow-bean-definition-overriding", () -> "true");
    }

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();

        clienteRepository.deleteAll();
    }

    @AfterEach
    void verificarContainerAtivo() {
        assertThat(POSTGRES.isRunning())
                .as("Container PostgreSQL deve estar em execução")
                .isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Autorização
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Autorização")
    class AutorizacaoTests {

        @Test
        @DisplayName("deve retornar 401 quando requisição sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/api/clientes"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 401 com token JWT malformado")
        void deveRetornar401ComTokenMalformado() throws Exception {
            mockMvc.perform(get("/api/clientes")
                            .header("Authorization", "Bearer isto.nao.e.um.jwt.valido"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 401 com Bearer vazio")
        void deveRetornar401ComBearerVazio() throws Exception {
            mockMvc.perform(get("/api/clientes")
                            .header("Authorization", "Bearer "))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 403 quando MECANICO tenta criar cliente")
        void deveRetornar403QuandoMecanicoTentaCriarCliente() throws Exception {
            mockMvc.perform(post("/api/clientes/pf")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenMecanico())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoClientePF("João Mecânico", "52998224725",
                                    "joao@test.com", "85999990000")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 403 quando MECANICO tenta atualizar cliente")
        void deveRetornar403QuandoMecanicoTentaAtualizarCliente() throws Exception {
            Cliente salvo = clienteRepository.save(clientePF("Teste", "52998224725"));

            mockMvc.perform(put("/api/clientes/" + salvo.getId())
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenMecanico())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Novo Nome\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve permitir ADMIN listar clientes")
        void devePermitirAdminListarClientes() throws Exception {
            mockMvc.perform(get("/api/clientes")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve permitir ATENDENTE listar clientes")
        void devePermitirAtendenteListarClientes() throws Exception {
            mockMvc.perform(get("/api/clientes")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente()))
                    .andExpect(status().isOk());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/clientes/pf | /pj — Criação
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/clientes/pf | /pj — Criação")
    class CriacaoClienteTests {

        @Test
        @DisplayName("deve criar cliente PF com CPF válido")
        void deveCriarClientePfComCpfValido() throws Exception {
            mockMvc.perform(post("/api/clientes/pf")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoClientePF("Maria Silva", "52998224725",
                                    "maria@test.com", "85988887777")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id",        notNullValue()))
                    .andExpect(jsonPath("$.nome",      is("Maria Silva")))
                    .andExpect(jsonPath("$.documento", is("52998224725")));

            assertThat(clienteRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve criar cliente PJ com CNPJ válido")
        void deveCriarClientePjComCnpjValido() throws Exception {
            mockMvc.perform(post("/api/clientes/pj")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoClientePJ("Empresa LTDA", "11222333000181",
                                    "Empresa LTDA ME", "empresa@test.com", "85933334444")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome", is("Empresa LTDA")));
        }

        @Test
        @DisplayName("deve retornar 422 com CPF inválido — todos dígitos iguais")
        void deveRetornar422ComCpfComDigitosIguais() throws Exception {
            mockMvc.perform(post("/api/clientes/pf")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoClientePF("Inválido", "11111111111",
                                    "invalido@test.com", "85900000000")))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("deve retornar 422 com CPF com formato incorreto")
        void deveRetornar422ComCpfFormatoIncorreto() throws Exception {
            mockMvc.perform(post("/api/clientes/pf")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoClientePF("Inválido", "12345",
                                    "invalido@test.com", "85900000000")))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("deve retornar 400 quando nome está ausente")
        void deveRetornar400QuandoNomeAusente() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "cpf",                "52998224725",
                    "email",              "teste@test.com",
                    "telefone",           "85900000000",
                    "aceitaNotificacoes", true
            ));

            mockMvc.perform(post("/api/clientes/pf")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 422 ao cadastrar CPF já existente")
        void deveRetornar422AoCadastrarCpfJaExistente() throws Exception {
            mockMvc.perform(post("/api/clientes/pf")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoClientePF("João Original", "52998224725",
                                    "joao@test.com", "85900000001")))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/clientes/pf")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoClientePF("João Duplicado", "52998224725",
                                    "joao2@test.com", "85900000002")))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/clientes — Consulta
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clientes — Consulta")
    class ConsultaClienteTests {

        @Test
        @DisplayName("deve listar todos os clientes ativos")
        void deveListarTodosOsClientesAtivos() throws Exception {
            clienteRepository.save(clientePF("João Teste",  "52998224725"));
            clienteRepository.save(clientePF("Ana Teste",   "98765432100"));
            clienteRepository.save(clientePF("Pedro Teste", "11144477735"));

            mockMvc.perform(get("/api/clientes")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há clientes")
        void deveRetornarListaVaziaQuandoNaoHaClientes() throws Exception {
            mockMvc.perform(get("/api/clientes")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("deve buscar cliente por ID")
        void deveBuscarClientePorId() throws Exception {
            Cliente salvo = clienteRepository.save(clientePF("Carlos Busca", "52998224725"));

            mockMvc.perform(get("/api/clientes/" + salvo.getId())
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id",   is(salvo.getId().toString())))
                    .andExpect(jsonPath("$.nome", is("Carlos Busca")));
        }

        @Test
        @DisplayName("deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            mockMvc.perform(get("/api/clientes/00000000-0000-0000-0000-000000000000")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 400 para UUID malformado")
        void deveRetornar400ParaUuidMalformado() throws Exception {
            mockMvc.perform(get("/api/clientes/nao-e-um-uuid")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve buscar cliente por CPF")
        void deveBuscarClientePorCpf() throws Exception {
            clienteRepository.save(clientePF("Luiza CPF", "52998224725"));

            mockMvc.perform(get("/api/clientes/buscarDOC")
                            .param("documento", "52998224725")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome", is("Luiza CPF")));
        }

        @Test
        @DisplayName("não deve retornar clientes inativos na listagem geral")
        void naoDeveRetornarClientesInativosNaListagem() throws Exception {
            clienteRepository.save(clientePF("Ativo",    "52998224725"));
            clienteRepository.save(clienteInativo("Inativo", "98765432100"));

            mockMvc.perform(get("/api/clientes")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nome", is("Ativo")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/clientes/{id} — Atualização
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/clientes/{id} — Atualização")
    class AtualizacaoClienteTests {

        @Test
        @DisplayName("deve atualizar nome e email do cliente")
        void deveAtualizarNomeEEmailDoCliente() throws Exception {
            Cliente salvo = clienteRepository.save(clientePF("Nome Antigo", "52998224725"));

            String body = objectMapper.writeValueAsString(Map.of(
                    "nome",     "Nome Atualizado",
                    "email",    "novo.email@test.com",
                    "telefone", "85977776666"
            ));

            mockMvc.perform(put("/api/clientes/" + salvo.getId())
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome",  is("Nome Atualizado")))
                    .andExpect(jsonPath("$.email", is("novo.email@test.com")));

            Cliente atualizado = clienteRepository.findById(salvo.getId()).orElseThrow();
            assertThat(atualizado.getNome()).isEqualTo("Nome Atualizado");
        }

        @Test
        @DisplayName("deve retornar 404 ao atualizar cliente inexistente")
        void deveRetornar404AoAtualizarClienteInexistente() throws Exception {
            mockMvc.perform(put("/api/clientes/00000000-0000-0000-0000-000000000000")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("nome", "Qualquer"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMIN também pode atualizar cliente")
        void adminTambemPodeAtualizarCliente() throws Exception {
            Cliente salvo = clienteRepository.save(clientePF("Original", "52998224725"));

            mockMvc.perform(put("/api/clientes/" + salvo.getId())
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("nome", "Alterado pelo Admin"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome", is("Alterado pelo Admin")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/clientes/{id} — Desativação
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/clientes/{id} — Desativação")
    class DesativacaoClienteTests {

        @Test
        @DisplayName("deve desativar cliente — soft delete")
        void deveDesativarClienteSoftDelete() throws Exception {
            Cliente salvo = clienteRepository.save(clientePF("Para Desativar", "52998224725"));

            mockMvc.perform(delete("/api/clientes/" + salvo.getId())
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin()))
                    .andExpect(status().isNoContent());

            Cliente inativo = clienteRepository.findById(salvo.getId()).orElseThrow();
            assertThat(inativo.getAtivo()).isFalse();
        }

        @Test
        @DisplayName("deve retornar 404 ao desativar cliente inexistente")
        void deveRetornar404AoDesativarClienteInexistente() throws Exception {
            mockMvc.perform(delete("/api/clientes/00000000-0000-0000-0000-000000000000")
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 403 quando ATENDENTE tenta desativar cliente")
        void deveRetornar403QuandoAtendenteTentaDesativarCliente() throws Exception {
            Cliente salvo = clienteRepository.save(clientePF("Protegido", "52998224725"));

            mockMvc.perform(delete("/api/clientes/" + salvo.getId())
                            .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente()))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String corpoClientePF(String nome, String cpf,
                                  String email, String telefone) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "nome",               nome,
                "cpf",                cpf,
                "email",              email,
                "telefone",           telefone,
                "aceitaNotificacoes", true
        ));
    }

    private String corpoClientePJ(String nome, String cnpj, String razaoSocial,
                                  String email, String telefone) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "nome",               nome,
                "cnpj",               cnpj,
                "razaoSocial",        razaoSocial,
                "email",              email,
                "telefone",           telefone,
                "aceitaNotificacoes", true
        ));
    }

    private ClientePF clientePF(String nome, String cpf) {
        return ClientePF.builder()
                .nome(nome)
                .cpf(new CPF(cpf))
                .email(nome.toLowerCase().replace(" ", ".") + "@test.com")
                .telefone("85999990000")
                .ativo(true)
                .build();
    }

    private ClientePF clienteInativo(String nome, String cpf) {
        return ClientePF.builder()
                .nome(nome)
                .cpf(new CPF(cpf))
                .email(nome.toLowerCase().replace(" ", ".") + "@test.com")
                .telefone("85999990001")
                .ativo(false)
                .build();
    }
}
