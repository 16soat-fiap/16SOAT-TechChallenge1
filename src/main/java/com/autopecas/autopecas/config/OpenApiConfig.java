package com.autopecas.autopecas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Swagger/OpenAPI com suporte a autenticação via Keycloak.
 * <p>
 * Oferece dois esquemas de segurança no Swagger UI:
 * 1. BearerAuth     — colar o access_token JWT manualmente (mais simples)
 * 2. KeycloakOAuth2 — fluxo Authorization Code via Keycloak (integrado)
 * <p>
 * Para testar no Swagger UI:
 * - Clique em "Authorize"
 * - Use BearerAuth e cole o token obtido do Keycloak
 */
@Configuration
public class OpenApiConfig {

    @Value("${keycloak.auth-server-url:http://localhost:8081}")
    private String keycloakUrl;

    @Value("${keycloak.realm:autopecas}")
    private String realm;

    @Bean
    public OpenAPI autoPecasOpenAPI() {
        String authUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect";

        return new OpenAPI()
                .info(new Info()
                        .title("API Auto Peças & Oficina Mecânica")
                        .description("""
                                Sistema de gestão de ordens de serviço para oficina mecânica.
                                
                                **Autenticação:** JWT via Keycloak.
                                - Obtenha um token em `POST /realms/autopecas/protocol/openid-connect/token`
                                - Cole no botão **Authorize → BearerAuth**
                                
                                **Roles disponíveis:**
                                - `ADMIN`     — acesso total
                                - `ATENDENTE` — gestão de OS, clientes, veículos e orçamentos
                                - `MECANICO`  — atualização de status e execução de OS
                                - `CLIENTE`   — consulta de OS/veículos próprios e aprovação/rejeição de orçamentos
                                """)
                        .version("v1.0.0"))

                // ── Esquema 1: Bearer Token (JWT colado manualmente) ──────────────
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Cole o access_token JWT obtido do Keycloak"))

                        // ── Esquema 2: OAuth2 Authorization Code (fluxo completo) ─────
                        .addSecuritySchemes("KeycloakOAuth2",
                                new SecurityScheme()
                                        .name("KeycloakOAuth2")
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .flows(new OAuthFlows()
                                                .authorizationCode(new OAuthFlow()
                                                        .authorizationUrl(authUrl + "/auth")
                                                        .tokenUrl(authUrl + "/token")))));
    }
}