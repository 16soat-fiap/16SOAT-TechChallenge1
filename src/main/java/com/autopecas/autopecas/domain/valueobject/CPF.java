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
public class CPF {

    @Column(name = "cpf", nullable = false, length = 11)
    private String valor;

    public CPF(String valor) {
        String cpfLimpo = limpar(valor);

        if (!isValido(cpfLimpo)) {
            throw new BusinessException("CPF inválido");
        }

        this.valor = cpfLimpo;
    }

    public String formatado() {
        return valor.replaceFirst(
                "(\\d{3})(\\d{3})(\\d{3})(\\d{2})",
                "$1.$2.$3-$4"
        );
    }

    private String limpar(String cpf) {
        if (cpf == null) {
            throw new IllegalArgumentException("CPF não pode ser nulo");
        }

        return cpf.replaceAll("\\D", "").trim();
    }

    private boolean isValido(String cpf) {
        if (cpf.length() != 11) {
            return false;
        }

        if (cpf.matches("(\\d)\\1{10}")) {
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