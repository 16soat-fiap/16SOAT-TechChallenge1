package com.autopecas.autopecas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI autoPecasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Auto Peças & Oficina Mecânica")
                        .description("Documentação dos endpoints do sistema de gestão de oficina e ordens de serviço.")
                        .version("v0.0.1"));
    }
}