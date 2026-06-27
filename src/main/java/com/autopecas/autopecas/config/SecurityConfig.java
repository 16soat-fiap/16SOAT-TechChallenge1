package com.autopecas.autopecas.config;

import com.autopecas.autopecas.security.KeycloakJwtAuthConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracao de seguranca integrada ao Keycloak via OAuth2 Resource Server.
 *
 * Roles esperadas no token JWT (mapeadas do Keycloak):
 * - ROLE_ADMIN     -> acesso total
 * - ROLE_ATENDENTE -> criar/consultar OS, orcamentos, clientes e veiculos
 * - ROLE_MECANICO  -> atualizar status de OS, consultar pecas e servicos
 * - ROLE_CLIENTE   -> visualizar OS e veiculos proprios, aprovar/rejeitar orcamentos
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final KeycloakJwtAuthConverter keycloakJwtAuthConverter;

    public SecurityConfig(KeycloakJwtAuthConverter keycloakJwtAuthConverter) {
        this.keycloakJwtAuthConverter = keycloakJwtAuthConverter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // Documentacao e health check -- publicos
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()

                        // Endpoint publico para acompanhamento de OS sem autenticacao
                        .requestMatchers(HttpMethod.GET, "/api/ordens-servico/acompanhamento/**").permitAll()

                        // Gestao de funcionarios -- somente ADMIN
                        .requestMatchers("/api/funcionarios/**")
                        .hasRole("ADMIN")

                        // CRUD de clientes -- ADMIN e ATENDENTE
                        .requestMatchers("/api/clientes/**")
                        .hasAnyRole("ADMIN", "ATENDENTE")

                        // Consulta de veiculos -- ADMIN, ATENDENTE e CLIENTE
                        .requestMatchers(HttpMethod.GET, "/api/veiculos/**")
                        .hasAnyRole("ADMIN", "ATENDENTE", "CLIENTE")

                        // Criacao/atualizacao/exclusao de veiculos -- ADMIN e ATENDENTE
                        .requestMatchers("/api/veiculos/**")
                        .hasAnyRole("ADMIN", "ATENDENTE")

                        // Criacao de OS -- ADMIN e ATENDENTE
                        .requestMatchers(HttpMethod.POST, "/api/ordens-servico")
                        .hasAnyRole("ADMIN", "ATENDENTE")

                        // Aprovacao de orcamento -- ADMIN, ATENDENTE e CLIENTE (antes da regra geral de PATCH)
                        .requestMatchers(HttpMethod.PATCH, "/api/ordens-servico/*/orcamentos/*/aprovar")
                        .hasAnyRole("ADMIN", "ATENDENTE", "CLIENTE")

                        // Rejeicao de orcamento -- ADMIN, ATENDENTE e CLIENTE (antes da regra geral de PATCH)
                        .requestMatchers(HttpMethod.PATCH, "/api/ordens-servico/*/orcamentos/*/rejeitar")
                        .hasAnyRole("ADMIN", "ATENDENTE", "CLIENTE")

                        // Atualizacao de status e demais PATCHes de OS -- ADMIN, ATENDENTE e MECANICO
                        .requestMatchers(HttpMethod.PATCH, "/api/ordens-servico/**")
                        .hasAnyRole("ADMIN", "ATENDENTE", "MECANICO")

                        // Listagem e detalhe de OS -- ADMIN, ATENDENTE, MECANICO e CLIENTE
                        .requestMatchers(HttpMethod.GET, "/api/ordens-servico/**")
                        .hasAnyRole("ADMIN", "ATENDENTE", "MECANICO", "CLIENTE")

                        // Consulta de orcamentos -- ADMIN, ATENDENTE e CLIENTE
                        .requestMatchers(HttpMethod.GET, "/api/ordens-servico/*/orcamentos/**")
                        .hasAnyRole("ADMIN", "ATENDENTE", "CLIENTE")

                        // Criacao e envio de orcamentos -- ADMIN e ATENDENTE
                        .requestMatchers("/api/ordens-servico/*/orcamentos/**")
                        .hasAnyRole("ADMIN", "ATENDENTE")

                        // Consulta de servicos -- ADMIN, ATENDENTE e MECANICO
                        .requestMatchers(HttpMethod.GET, "/api/servicos/**")
                        .hasAnyRole("ADMIN", "ATENDENTE", "MECANICO")

                        // CRUD completo de servicos -- ADMIN e ATENDENTE
                        .requestMatchers("/api/servicos/**")
                        .hasAnyRole("ADMIN", "ATENDENTE")

                        // Consulta de pecas -- ADMIN, ATENDENTE e MECANICO
                        .requestMatchers(HttpMethod.GET, "/api/pecas/**")
                        .hasAnyRole("ADMIN", "TENDENTE", "MECANICO")

                        // CRUD completo de pecas e estoque -- ADMIN e ATENDENTE
                        .requestMatchers("/api/pecas/**")
                        .hasAnyRole("ADMIN", "ATENDENTE")

                        // Dashboard e relatorios -- somente ADMIN
                        .requestMatchers("/api/dashboard/**")
                        .hasRole("ADMIN")

                        // Qualquer outra rota exige autenticacao
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(keycloakJwtAuthConverter)));

        return http.build();
    }
}
