package com.autopecas.autopecas.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Port de saída para a leitura do tempo.
 *
 * <p>Existe para que o domínio nunca chame LocalDateTime.now() diretamente: o caso de uso
 * obtém o instante daqui e o passa como parâmetro para os métodos de domínio, o que torna
 * as regras determinísticas e testáveis com um relógio fixo.
 */
public interface Relogio {

    LocalDateTime agora();

    default LocalDate hoje() {
        return agora().toLocalDate();
    }
}
