package com.autopecas.autopecas.domain.vo;

/**
 * Value Object de CNPJ: normalizado para 14 dígitos e validado pelos dígitos verificadores.
 *
 * <p>Nota: lança IllegalArgumentException (e não BusinessException) para manter o
 * comportamento HTTP idêntico ao da versão anterior ao refactor hexagonal.
 */
public record CNPJ(String valor) {

    private static final int[] PESO_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESO_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public CNPJ {
        valor = limpar(valor);
        if (!isValido(valor)) {
            throw new IllegalArgumentException("CNPJ inválido");
        }
    }

    public String formatado() {
        return valor.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    @Override
    public String toString() {
        return formatado();
    }

    private static String limpar(String cnpj) {
        if (cnpj == null) {
            throw new IllegalArgumentException("CNPJ não pode ser nulo");
        }
        return cnpj.replaceAll("\\D", "").trim();
    }

    private static boolean isValido(String cnpj) {
        if (cnpj.length() != 14 || cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        int digito1 = digitoVerificador(cnpj, PESO_1, 12);
        int digito2 = digitoVerificador(cnpj, PESO_2, 13);

        return digito1 == Character.getNumericValue(cnpj.charAt(12))
                && digito2 == Character.getNumericValue(cnpj.charAt(13));
    }

    private static int digitoVerificador(String cnpj, int[] pesos, int digitos) {
        int soma = 0;
        for (int i = 0; i < digitos; i++) {
            soma += Character.getNumericValue(cnpj.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
