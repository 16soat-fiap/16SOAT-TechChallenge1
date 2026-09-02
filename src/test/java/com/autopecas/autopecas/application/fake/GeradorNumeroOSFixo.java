package com.autopecas.autopecas.application.fake;

import com.autopecas.autopecas.application.port.out.GeradorNumeroOS;

import java.util.concurrent.atomic.AtomicInteger;

/** Numeração determinística de OS, no mesmo formato do adapter Postgres. */
public class GeradorNumeroOSFixo implements GeradorNumeroOS {

    private final AtomicInteger sequencia = new AtomicInteger(0);

    @Override
    public String proximo() {
        return String.format("OS-%06d", sequencia.incrementAndGet());
    }
}
