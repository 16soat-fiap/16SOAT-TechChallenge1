package com.autopecas.autopecas.utils;

public class DocumentValidator {

    public static boolean validarCPF(String cpf) {
        if (cpf == null) return false;

        // Remove caracteres não numéricos
        cpf = cpf.replaceAll("\\D", "");

        // Verifica tamanho e sequências repetidas (ex: 00000000000)
        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) return false;

        try {
            int soma = 0, peso = 10;
            for (int i = 0; i < 9; i++) {
                int num = cpf.charAt(i) - '0';
                soma += (num * peso);
                peso--;
            }
            int resto = 11 - (soma % 11);
            char digito10 = (resto == 10 || resto == 11) ? '0' : (char) (resto + '0');

            soma = 0;
            peso = 11;
            for (int i = 0; i < 10; i++) {
                int num = cpf.charAt(i) - '0';
                soma += (num * peso);
                peso--;
            }
            resto = 11 - (soma % 11);
            char digito11 = (resto == 10 || resto == 11) ? '0' : (char) (resto + '0');

            return (digito10 == cpf.charAt(9)) && (digito11 == cpf.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validarCNPJ(String cnpj) {
        if (cnpj == null) return false;

        cnpj = cnpj.replaceAll("\\D", "");

        if (cnpj.length() != 14 || cnpj.matches("(\\d)\\1{13}")) return false;

        try {
            int soma = 0, peso = 2;
            for (int i = 11; i >= 0; i--) {
                int num = cnpj.charAt(i) - '0';
                soma += (num * peso);
                peso++;
                if (peso == 10) peso = 2;
            }
            int resto = soma % 11;
            char digito13 = (resto == 0 || resto == 1) ? '0' : (char) ((11 - resto) + '0');

            soma = 0;
            peso = 2;
            for (int i = 12; i >= 0; i--) {
                int num = cnpj.charAt(i) - '0';
                soma += (num * peso);
                peso++;
                if (peso == 10) peso = 2;
            }
            resto = soma % 11;
            char digito14 = (resto == 0 || resto == 1) ? '0' : (char) ((11 - resto) + '0');

            return (digito13 == cnpj.charAt(12)) && (digito14 == cnpj.charAt(13));
        } catch (Exception e) {
            return false;
        }
    }
}
