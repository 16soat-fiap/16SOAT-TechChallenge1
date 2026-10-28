package com.autopecas.autopecas.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class ProductionConfigurationTest {
    @Test
    void productionUsesSharedJwtConfigurationAndOnlySchemaMigrations() throws Exception {
        var loader = new YamlPropertySourceLoader();
        var production =
                loader.load("production", new ClassPathResource("application-prod.yml")).getFirst();
        assertThat(production.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration");
        assertThat(production.getProperty("server.port")).isEqualTo(8080);
        assertThat(production.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        var source = new ClassPathResource("application-prod.yml");
        try (var stream = source.getInputStream()) {
            assertThat(
                            new String(
                                            stream.readAllBytes(),
                                            java.nio.charset.StandardCharsets.UTF_8)
                                    .toLowerCase())
                    .doesNotContain("keycloak", "oauth2_jwk_uri", "oauth2_issuer_uri");
        }
    }
}
