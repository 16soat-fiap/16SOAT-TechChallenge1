package com.autopecas.autopecas.adapter.out.clock;

import com.autopecas.autopecas.application.port.out.Relogio;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Implementação da port Relogio sobre o relógio do sistema.
 *
 * <p>Recebe um java.time.Clock por construtor para que os testes possam fixar o tempo sem
 * precisar de mock estático.
 */
@Component
public class RelogioSistema implements Relogio {

    private final Clock clock;

    public RelogioSistema() {
        this(Clock.systemDefaultZone());
    }

    public RelogioSistema(Clock clock) {
        this.clock = clock;
    }

    @Override
    public LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
