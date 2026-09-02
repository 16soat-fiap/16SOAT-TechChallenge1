package com.autopecas.autopecas.domain.vo;

import com.autopecas.autopecas.domain.exception.BusinessException;

/**
 * Value Object de CPF: normalizado para 11 dígitos e validado pelos dígitos verificadores.
 *
 * <p>Imutável e comparado pelo conteúdo — dois CPF com o mesmo número são iguais.
 */
public record CPF(String valor) {

    public CPF {
        valor = limpar(valor);
        if (!isValido(valor)) {
            throw new BusinessException("CPF inválido");
        }
    }

    public String formatado() {
        return valor.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    @Override
    public String toString() {
        return valor;
    }

    private static String limpar(String cpf) {
        if (cpf == null) {
            throw new IllegalArgumentException("CPF não pode ser nulo");
        }
        return cpf.replaceAll("\\D", "").trim();
    }

    private static boolean isValido(String cpf) {
        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int resto = 11 - (soma % 11);
        int digito1 = resto >= 10 ? 0 : resto;

        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        resto = 11 - (soma % 11);
        int digito2 = resto >= 10 ? 0 : resto;

        return digito1 == Character.getNumericValue(cpf.charAt(9))
                && digito2 == Character.getNumericValue(cpf.charAt(10));
    }
}
