package com.autopecas.autopecas.config;

import com.autopecas.autopecas.security.KeycloakJwtAuthConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /**
     * Probes de saúde, liberadas sem autenticação.
     * <p>
     * O kubelet chama estes endpoints sem credencial alguma; exigir token aqui faria toda
     * readiness/liveness responder 401 e o pod entrar em CrashLoopBackOff. O mesmo vale para o
     * healthcheck do Docker Compose.
     * <p>
     * O que vaza é apenas o estado UP/DOWN: o profile prod expõe somente `health` e `info`, e
     * com `show-details: never` a resposta não revela detalhes dos componentes.
     */
    private static final String[] HEALTH_WHITELIST = {
            "/actuator/health",
            "/actuator/health/**"
    };

    /**
     * O converter do Keycloak entra aqui por injeção. Ele é a única fonte de roles e do nome do
     * principal — a autorização por @PreAuthorize e a identificação do usuário por e-mail
     * dependem das duas coisas saírem do mesmo lugar.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           KeycloakJwtAuthConverter jwtAuthConverter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HEALTH_WHITELIST).permitAll()
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                );

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder() {
        // Dynamically injected URLs handle the Docker networking split
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);

        jwtDecoder.setJwtValidator(withIssuer);
        return jwtDecoder;
    }
}
