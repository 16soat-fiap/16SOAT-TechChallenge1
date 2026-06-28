package com.autopecas.autopecas.util.test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utilitários para geração de tokens JWT em testes de integração.
 *
 * Gera par de chaves RSA em memória (sem Keycloak real) e assina tokens
 * com a estrutura de claims que o KeycloakJwtAuthConverter espera.
 *
 * Uso nos testes:
 * <pre>
 *   mockMvc.perform(get("/api/clientes")
 *       .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin()))
 * </pre>
 *
 * Ou com @WithMockUser + @WithSecurityContext quando não precisar do HTTP real.
 */
public final class JwtTestUtils {

    // Par de chaves RSA gerado uma única vez por JVM de teste
    public static final KeyPair KEY_PAIR = gerarKeyPair();
    public static final RSAPublicKey PUBLIC_KEY = (RSAPublicKey) KEY_PAIR.getPublic();
    public static final RSAPrivateKey PRIVATE_KEY = (RSAPrivateKey) KEY_PAIR.getPrivate();

    private static final String ISSUER = "http://localhost:9999/realms/autopecas";

    private JwtTestUtils() {}

    // ─── Tokens prontos por role ───────────────────────────────────────────

    public static String tokenAdmin() {
        return assinarToken("admin-test", "admin.user", List.of("ADMIN"));
    }

    public static String tokenAtendente() {
        return assinarToken("atendente-test", "ana.atendente", List.of("ATENDENTE"));
    }

    public static String tokenMecanico() {
        return assinarToken("mecanico-test", "joao.mecanico", List.of("MECANICO"));
    }

    public static String tokenComRoles(List<String> roles) {
        return assinarToken(UUID.randomUUID().toString(), "usuario.teste", roles);
    }

    // ─── JwtAuthenticationToken para usar com SecurityMockMvcRequestPostProcessors ──

    public static JwtAuthenticationToken authenticationAdmin() {
        return buildAuthentication("admin-test", "admin.user", List.of("ADMIN"));
    }

    public static JwtAuthenticationToken authenticationAtendente() {
        return buildAuthentication("atendente-test", "ana.atendente", List.of("ATENDENTE"));
    }

    public static JwtAuthenticationToken authenticationMecanico() {
        return buildAuthentication("mecanico-test", "joao.mecanico", List.of("MECANICO"));
    }

    // ─── Implementação interna ─────────────────────────────────────────────

    /**
     * Assina um token JWT com RS256 no formato esperado pelo Keycloak.
     * Os roles ficam em realm_access.roles.
     */
    public static String assinarToken(String subject, String username, List<String> roles) {
        try {
            JWSSigner signer = new RSASSASigner(PRIVATE_KEY);

            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject(subject)
                    .audience("autopecas-api")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("preferred_username", username)
                    .claim("email", username + "@test.com")
                    // Estrutura de roles do Keycloak
                    .claim("realm_access", Map.of("roles", roles))
                    .claim("resource_access", Map.of(
                            "autopecas-api", Map.of("roles", roles)
                    ))
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),
                    claims
            );
            signedJWT.sign(signer);
            return signedJWT.serialize();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar token JWT de teste", e);
        }
    }

    private static JwtAuthenticationToken buildAuthentication(String sub, String username, List<String> roles) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("mock-token-" + sub)
                .header("alg", "RS256")
                .issuer(ISSUER)
                .subject(sub)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("preferred_username", username)
                .claim("realm_access", Map.of("roles", roles))
                .build();

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                .collect(Collectors.toList());

        return new JwtAuthenticationToken(jwt, authorities, sub);
    }

    private static KeyPair gerarKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar KeyPair RSA para testes", e);
        }
    }
}