package com.autopecas.autopecas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.autopecas.autopecas.util.test.JwtTestUtils;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

class LambdaJwtIntegrationTest extends IntegrationTestBase {
    private static final UUID CLIENTE = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OUTRO = UUID.fromString("10000000-0000-0000-0000-000000000002");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        inserir(CLIENTE, "52998224725");
        inserir(OUTRO, "11144477735");
    }

    private void inserir(UUID id, String cpf) {
        jdbc.update(
                "INSERT INTO clientes(id,tipo_cliente,nome,ativo,created_at,update_at) VALUES"
                    + " (?,'PF','Teste identidade',true,now(),now())",
                id);
        jdbc.update("INSERT INTO clientes_pf(id,cpf) VALUES (?,?)", id, cpf);
    }

    private String token(String perfil, List<String> roles, String audience, boolean legacy)
            throws Exception {
        var claims =
                new JWTClaimsSet.Builder()
                        .issuer("https://auth.example.test")
                        .audience(audience)
                        .subject(CLIENTE.toString())
                        .issueTime(Date.from(Instant.now()))
                        .expirationTime(Date.from(Instant.now().plusSeconds(1800)))
                        .claim("scope", "api:access")
                        .claim("perfil", perfil)
                        .claim("roles", roles)
                        .claim("tipo", "PF");
        if (legacy) claims.claim("realm_access", Map.of("roles", List.of("ADMIN")));
        var signed =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),
                        claims.build());
        signed.sign(new RSASSASigner(JwtTestUtils.PRIVATE_KEY));
        return signed.serialize();
    }

    @Test
    void clienteSoAcessaProprioCadastroENaoViraAdminPorClaimLegada() throws Exception {
        String jwt = token("CLIENTE", List.of("CLIENTE"), "autopecas-api", true);
        mvc.perform(get("/api/clientes/" + CLIENTE).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        mvc.perform(get("/api/clientes/" + OUTRO).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/funcionarios").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteInativoNaoPassaNaChecagemDePropriedade() throws Exception {
        jdbc.update("UPDATE clientes SET ativo=false WHERE id=?", CLIENTE);
        mvc.perform(
                        get("/api/clientes/" + CLIENTE)
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + token(
                                                        "CLIENTE",
                                                        List.of("CLIENTE"),
                                                        "autopecas-api",
                                                        false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void perfilClienteNaoPodeCarregarRolesDeFuncionario() throws Exception {
        mvc.perform(
                        get("/api/dashboard")
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + token(
                                                        "CLIENTE",
                                                        List.of("ADMIN"),
                                                        "autopecas-api",
                                                        false)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void audienceIncorretaEPerfilDesconhecidoSaoRejeitados() throws Exception {
        mvc.perform(
                        get("/api/clientes/" + CLIENTE)
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + token(
                                                        "CLIENTE",
                                                        List.of("CLIENTE"),
                                                        "outra-api",
                                                        false)))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/clientes/" + CLIENTE)
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + token(
                                                        "DESCONHECIDO",
                                                        List.of("CLIENTE"),
                                                        "autopecas-api",
                                                        false)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permissoesDeFuncionariosPermanecemDiferentes() throws Exception {
        mvc.perform(
                        get("/api/clientes")
                                .header("Authorization", "Bearer " + JwtTestUtils.tokenAtendente()))
                .andExpect(status().isOk());
        mvc.perform(
                        get("/api/clientes")
                                .header("Authorization", "Bearer " + JwtTestUtils.tokenMecanico()))
                .andExpect(status().isForbidden());
        mvc.perform(
                        get("/api/dashboard")
                                .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin()))
                .andExpect(status().isOk());
    }
}
