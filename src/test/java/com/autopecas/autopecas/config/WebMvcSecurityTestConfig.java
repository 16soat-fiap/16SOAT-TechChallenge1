package com.autopecas.autopecas.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@TestConfiguration
public class WebMvcSecurityTestConfig {

    @Bean
    MockMvcBuilderCustomizer adminJwtCustomizer() {
        return builder -> builder.defaultRequest(
            MockMvcRequestBuilders.get("/")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
    }
}
