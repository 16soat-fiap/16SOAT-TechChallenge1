package com.autopecas.autopecas.domain.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CNPJ {

    @Column(name = "cnpj", nullable = false, length = 14)
    private String valor;

    public CNPJ(String valor) {
        String cnpjLimpo = limpar(valor);

        if (!isValido(cnpjLimpo)) {
            throw new IllegalArgumentException("CNPJ inválido");
        }

        this.valor = cnpjLimpo;
    }

    @Override
    public String toString() {
        return formatado();
    }

    public String formatado() {
        return valor.replaceFirst(
                "(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})",
                "$1.$2.$3/$4-$5"
        );
    }

    private String limpar(String cnpj) {
        if (cnpj == null) {
            throw new IllegalArgumentException("CNPJ não pode ser nulo");
        }

        return cnpj.replaceAll("\\D", "").trim();
    }

    private boolean isValido(String cnpj) {
        if (cnpj.length() != 14) {
            return false;
        }

        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += Character.getNumericValue(cnpj.charAt(i)) * peso1[i];
        }

        int resto = soma % 11;
        int digito1 = resto < 2 ? 0 : 11 - resto;

        soma = 0;
        for (int i = 0; i < 13; i++) {
            int numero = Character.getNumericValue(cnpj.charAt(i));
            soma += numero * peso2[i];
        }

        resto = soma % 11;
        int digito2 = resto < 2 ? 0 : 11 - resto;

        return digito1 == Character.getNumericValue(cnpj.charAt(12))
                && digito2 == Character.getNumericValue(cnpj.charAt(13));
    }
}