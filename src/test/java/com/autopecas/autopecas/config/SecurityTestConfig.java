package com.autopecas.autopecas.config;

import com.autopecas.autopecas.util.test.JwtTestUtils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Configuração de segurança para testes de integração.
 *
 * <p>Substitui o JwtDecoder padrão (que chamaria o emissor Lambda real via JWKS) por um decoder
 * local que usa a chave pública RSA gerada em memória pelo JwtTestUtils.
 *
 * <p>O @Primary garante que este bean tenha prioridade sobre o auto-configurado pelo Spring
 * Security OAuth2.
 *
 * <p>Adicione @Import(SecurityTestConfig.class) nas suas classes de teste de integração.
 */
@TestConfiguration
public class SecurityTestConfig {

    /**
     * JwtDecoder que valida tokens assinados com a chave privada do JwtTestUtils. Não faz chamadas
     * HTTP externas ao emissor Lambda.
     */
    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        var decoder = NimbusJwtDecoder.withPublicKey(JwtTestUtils.PUBLIC_KEY).build();
        decoder.setJwtValidator(
                new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                        org.springframework.security.oauth2.jwt.JwtValidators
                                .createDefaultWithIssuer("https://auth.example.test"),
                        new com.autopecas.autopecas.security.IdentidadeJwtValidator(
                                "autopecas-api")));
        return decoder;
    }
}
