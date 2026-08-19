package com.autopecas.autopecas.domain.vo;

/** Value Object de endereço. Opcional em Cliente e Funcionario. */
public record Endereco(
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf
) {
}
