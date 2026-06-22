package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.Atendente;
import com.autopecas.autopecas.domain.entity.Mecanico;
import com.autopecas.autopecas.domain.valueobject.Endereco;

import java.time.LocalDate;
import java.util.UUID;

public class FuncionarioBuilder {

    public static Mecanico.MecanicoBuilder<?, ?> mecanico() {
        return Mecanico.builder()
                .id(UUID.randomUUID())
                .matricula("MEC001")
                .cpf("11122233344")
                .nome("Mecânico Teste")
                .email("mecanico.teste@autopecas.com")
                .telefone("11998877665")
                .dataNascimento(LocalDate.of(1985, 10, 15))
                .ativo(true)
                .endereco(Endereco.builder()
                        .cep("03000-000")
                        .logradouro("Rua dos Mecânicos")
                        .numero("10")
                        .bairro("Oficina")
                        .cidade("Sao Paulo")
                        .uf("SP")
                        .build());
    }

    public static Atendente.AtendenteBuilder<?, ?> atendente() {
        return Atendente.builder()
                .id(UUID.randomUUID())
                .matricula("ATE001")
                .cpf("55566677788")
                .nome("Atendente Teste")
                .email("atendente.teste@autopecas.com")
                .telefone("11977665544")
                .dataNascimento(LocalDate.of(1992, 3, 20))
                .ativo(true)
                .endereco(Endereco.builder()
                        .cep("04000-000")
                        .logradouro("Av. do Atendimento")
                        .numero("200")
                        .bairro("Recepcao")
                        .cidade("Sao Paulo")
                        .uf("SP")
                        .build());
    }
}
