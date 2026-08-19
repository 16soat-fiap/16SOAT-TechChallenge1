package com.autopecas.autopecas.application.fake;

import com.autopecas.autopecas.application.port.out.GeradorMatricula;

import java.util.concurrent.atomic.AtomicInteger;

/** Matrículas determinísticas, no mesmo formato do adapter Postgres. */
public class GeradorMatriculaFixo implements GeradorMatricula {

    private final AtomicInteger mecanicos = new AtomicInteger(0);
    private final AtomicInteger atendentes = new AtomicInteger(0);

    @Override
    public String proximaDeMecanico() {
        return String.format("MEC-%04d", mecanicos.incrementAndGet());
    }

    @Override
    public String proximaDeAtendente() {
        return String.format("ATD-%04d", atendentes.incrementAndGet());
    }
}
