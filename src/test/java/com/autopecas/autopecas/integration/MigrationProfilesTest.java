package com.autopecas.autopecas.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

class MigrationProfilesTest extends IntegrationTestBase {
    @Autowired DataSource dataSource;

    @Test
    void migrationsDeProducaoEDemoNaoColidem() {
        var result =
                Flyway.configure()
                        .dataSource(dataSource)
                        .schemas("validacao_demo")
                        .defaultSchema("validacao_demo")
                        .locations("classpath:db/migration", "classpath:db/demo")
                        .load()
                        .migrate();
        assertEquals(4, result.migrationsExecuted);
    }
}
