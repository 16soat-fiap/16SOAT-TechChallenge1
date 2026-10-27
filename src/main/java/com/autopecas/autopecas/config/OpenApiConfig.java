package com.autopecas.autopecas.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;

import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI autoPecasOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("API Autopecas")
                                .version("v1.0.0")
                                .description(
                                        "JWT emitido pela Lambda: clientes em POST /auth;"
                                            + " funcionarios em POST /auth/funcionarios. Cole o"
                                            + " token em BearerAuth."))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "BearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
