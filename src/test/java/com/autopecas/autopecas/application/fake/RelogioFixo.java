package com.autopecas.autopecas.application.fake;

import com.autopecas.autopecas.application.port.out.Relogio;

import java.time.LocalDateTime;

/** Relógio parado num instante conhecido, para asserções exatas sobre datas. */
public class RelogioFixo implements Relogio {

    public static final LocalDateTime INSTANTE = LocalDateTime.of(2026, 3, 10, 14, 30);

    private final LocalDateTime agora;

    public RelogioFixo() {
        this(INSTANTE);
    }

    public RelogioFixo(LocalDateTime agora) {
        this.agora = agora;
    }

    @Override
    public LocalDateTime agora() {
        return agora;
    }
}
