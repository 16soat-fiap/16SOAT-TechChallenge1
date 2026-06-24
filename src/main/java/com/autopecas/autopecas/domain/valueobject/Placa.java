package com.autopecas.autopecas.domain.valueobject;

import com.autopecas.autopecas.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode
public class Placa {

    private static final String REGEX_ANTIGA = "^[A-Z]{3}[0-9]{4}$";
    private static final String REGEX_MERCOSUL = "^[A-Z]{3}[0-9][A-Z][0-9]{2}$";

    @Column(name = "placa", nullable = false, length = 7)
    private String valor;

    public Placa(String valor) {
        String placaLimpa = formatar(valor);

        if (!isValido(placaLimpa)) {
            throw new BusinessException("Placa inválida");
        }

        this.valor = placaLimpa;
    }

    public String formatado() {
        return valor.replaceFirst("([A-Z]{3})([0-9A-Z]{4})", "$1-$2");
    }

    private String formatar(String placa) {
        if (placa == null) {
            throw new IllegalArgumentException("Placa não pode ser nula");
        }

        return placa.replace("-", "").replace(" ", "").toUpperCase().trim();
    }

    private boolean isValido(String placa) {
        return placa.matches(REGEX_ANTIGA) || placa.matches(REGEX_MERCOSUL);
    }
}