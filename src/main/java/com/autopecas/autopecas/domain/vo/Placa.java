package com.autopecas.autopecas.domain.vo;

import com.autopecas.autopecas.domain.exception.BusinessException;

/**
 * Value Object de placa veicular, normalizada em maiúsculas e sem hífen.
 * Aceita o formato antigo (ABC1234) e o Mercosul (ABC1D23).
 */
public record Placa(String valor) {

    private static final String REGEX_ANTIGA = "^[A-Z]{3}[0-9]{4}$";
    private static final String REGEX_MERCOSUL = "^[A-Z]{3}[0-9][A-Z][0-9]{2}$";

    public Placa {
        valor = normalizar(valor);
        if (!isValido(valor)) {
            throw new BusinessException("Placa inválida");
        }
    }

    public String formatado() {
        return valor.replaceFirst("([A-Z]{3})([0-9A-Z]{4})", "$1-$2");
    }

    @Override
    public String toString() {
        return valor;
    }

    private static String normalizar(String placa) {
        if (placa == null) {
            throw new IllegalArgumentException("Placa não pode ser nula");
        }
        return placa.replace("-", "").replace(" ", "").toUpperCase().trim();
    }

    private static boolean isValido(String placa) {
        return placa.matches(REGEX_ANTIGA) || placa.matches(REGEX_MERCOSUL);
    }
}
