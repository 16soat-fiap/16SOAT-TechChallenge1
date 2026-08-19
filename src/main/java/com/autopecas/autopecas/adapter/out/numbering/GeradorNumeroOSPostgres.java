package com.autopecas.autopecas.adapter.out.numbering;

import com.autopecas.autopecas.adapter.out.persistence.repository.OrdemServicoJpaRepository;
import com.autopecas.autopecas.application.port.out.GeradorNumeroOS;
import org.springframework.stereotype.Component;

/**
 * Numeração de OS no formato OS-000001, obtida da sequence os_numero_seq do Postgres.
 *
 * <p>Usar a sequence do banco (em vez de MAX+1) evita colisão entre requisições concorrentes.
 */
@Component
public class GeradorNumeroOSPostgres implements GeradorNumeroOS {

    private static final String FORMATO = "OS-%06d";

    private final OrdemServicoJpaRepository repository;

    public GeradorNumeroOSPostgres(OrdemServicoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public String proximo() {
        return String.format(FORMATO, repository.proximoNumero());
    }
}
