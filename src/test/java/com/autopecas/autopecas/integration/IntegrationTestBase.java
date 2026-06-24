package com.autopecas.autopecas.integration;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Classe base para todos os testes de integração.
 *
 * Estratégia:
 *  - Um único contêiner PostgreSQL iniciado uma vez para toda a JVM (static block),
 *    compartilhado entre todas as subclasses via Spring context cache.
 *  - @SpringBootTest(MOCK) + @AutoConfigureMockMvc: contexto completo com MockMvc.
 *  - @Transactional: cada método de teste tem rollback automático -> isolamento total.
 *  - spring.docker.compose.enabled=false: evita que o Spring Boot tente iniciar o
 *    docker-compose.yml durante os testes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "spring.docker.compose.enabled=false")
abstract class IntegrationTestBase {

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
}
