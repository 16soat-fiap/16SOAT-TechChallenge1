package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.Cliente;
import com.autopecas.autopecas.domain.entity.ClientePF;
import com.autopecas.autopecas.domain.entity.ClientePJ;
import com.autopecas.autopecas.domain.enums.Genero;
import com.autopecas.autopecas.domain.valueobject.CNPJ;
import com.autopecas.autopecas.domain.valueobject.CPF;
import com.autopecas.autopecas.domain.valueobject.Endereco;

import java.time.LocalDate;
import java.util.UUID;

public class ClienteBuilder {

    public static ClientePF.ClientePFBuilder<?, ?> clientePF() {
        return ClientePF.builder()
                .id(UUID.randomUUID())
                .nome("Cliente PF Teste")
                .email("cliente.pf@teste.com")
                .telefone("11987654321")
                .aceitaNotificacoes(true)
                .ativo(true)
                .endereco(Endereco.builder()
                        .cep("01000-000")
                        .logradouro("Rua Teste PF")
                        .numero("123")
                        .bairro("Centro")
                        .cidade("Sao Paulo")
                        .uf("SP")
                        .build())
                .cpf(new CPF("52998224725"))
                .dataNascimento(LocalDate.of(1990, 1, 1))
                .rg("123456789")
                .genero(Genero.MASCULINO)
                .profissao("Engenheiro");
    }

    public static ClientePJ.ClientePJBuilder<?, ?> clientePJ() {
        return ClientePJ.builder()
                .id(UUID.randomUUID())
                .nome("Cliente PJ Teste")
                .email("cliente.pj@teste.com")
                .telefone("11912345678")
                .aceitaNotificacoes(true)
                .ativo(true)
                .endereco(Endereco.builder()
                        .cep("02000-000")
                        .logradouro("Av. Teste PJ")
                        .numero("456")
                        .bairro("Industrial")
                        .cidade("Sao Paulo")
                        .uf("SP")
                        .build())
                .cnpj(new CNPJ("12345678000195"))
                .razaoSocial("Empresa Teste LTDA")
                .inscricaoEstadual("ISENTA");
    }
}