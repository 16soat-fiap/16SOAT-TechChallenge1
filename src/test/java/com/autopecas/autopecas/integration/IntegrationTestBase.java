package com.autopecas.autopecas.integration;

import com.autopecas.autopecas.config.SecurityTestConfig;
import com.autopecas.autopecas.util.test.JwtTestUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.docker.compose.enabled=false",
    "spring.main.allow-bean-definition-overriding=true"
})
@Import({SecurityTestConfig.class, IntegrationTestBase.DefaultAuthConfig.class})
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class DefaultAuthConfig {
        @Bean
        MockMvcBuilderCustomizer adminTokenCustomizer() {
            return builder -> builder.defaultRequest(
                MockMvcRequestBuilders.get("/")
                    .header("Authorization", "Bearer " + JwtTestUtils.tokenAdmin())
            );
        }
    }
}
