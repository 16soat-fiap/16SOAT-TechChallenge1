package com.autopecas.autopecas.config;

import com.autopecas.autopecas.util.test.JwtTestUtils;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Configuração de segurança para testes de integração.
 *
 * Substitui o JwtDecoder padrão (que chamaria o Keycloak real via JWKS)
 * por um decoder local que usa a chave pública RSA gerada em memória
 * pelo JwtTestUtils.
 *
 * O @Primary garante que este bean tenha prioridade sobre o auto-configurado
 * pelo Spring Security OAuth2.
 *
 * Adicione @Import(SecurityTestConfig.class) nas suas classes de teste de integração.
 */
@TestConfiguration
public class SecurityTestConfig {

    /**
     * JwtDecoder que valida tokens assinados com a chave privada do JwtTestUtils.
     * Não faz chamadas HTTP externas ao Keycloak.
     */
    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        RSAKey rsaKey = new RSAKey.Builder(JwtTestUtils.PUBLIC_KEY)
                .privateKey(JwtTestUtils.PRIVATE_KEY)
                .keyID("test-key")
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);

        return NimbusJwtDecoder
                .withPublicKey(JwtTestUtils.PUBLIC_KEY)
                .build();
    }
}