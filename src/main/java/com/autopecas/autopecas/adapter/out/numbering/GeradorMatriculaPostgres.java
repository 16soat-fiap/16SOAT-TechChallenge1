package com.autopecas.autopecas.adapter.out.numbering;

import com.autopecas.autopecas.adapter.out.persistence.repository.FuncionarioJpaRepository;
import com.autopecas.autopecas.application.port.out.GeradorMatricula;
import org.springframework.stereotype.Component;

/**
 * Matrículas nos formatos MEC-0001 e ATD-0001, obtidas das sequences mecanico_seq e
 * atendente_seq do Postgres.
 */
@Component
public class GeradorMatriculaPostgres implements GeradorMatricula {

    private static final String FORMATO_MECANICO = "MEC-%04d";
    private static final String FORMATO_ATENDENTE = "ATD-%04d";

    private final FuncionarioJpaRepository repository;

    public GeradorMatriculaPostgres(FuncionarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public String proximaDeMecanico() {
        return String.format(FORMATO_MECANICO, repository.proximoNumeroMecanico());
    }

    @Override
    public String proximaDeAtendente() {
        return String.format(FORMATO_ATENDENTE, repository.proximoNumeroAtendente());
    }
}
