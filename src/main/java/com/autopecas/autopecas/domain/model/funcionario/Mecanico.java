package com.autopecas.autopecas.domain.model.funcionario;

import com.autopecas.autopecas.domain.enums.TipoFuncionario;
import com.autopecas.autopecas.domain.vo.Endereco;

import java.time.LocalDate;
import java.util.UUID;

/** Mecânico — funcionário que executa os serviços das ordens de serviço. */
public final class Mecanico extends Funcionario {

    private Mecanico(UUID id, String matricula, String cpf, String nome, String email,
                     String telefone, LocalDate dataNascimento, boolean ativo, Endereco endereco) {
        super(id, matricula, cpf, nome, email, telefone, dataNascimento, ativo, endereco);
    }

    /** Novo mecânico, ainda sem id. A matrícula vem do gerador de matrículas. */
    public static Mecanico criar(String matricula, String cpf, String nome, String email,
                                 String telefone, LocalDate dataNascimento) {
        return new Mecanico(null, matricula, cpf, nome, email, telefone, dataNascimento, true, null);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static Mecanico reconstituir(UUID id, String matricula, String cpf, String nome, String email,
                                        String telefone, LocalDate dataNascimento, boolean ativo,
                                        Endereco endereco) {
        return new Mecanico(id, matricula, cpf, nome, email, telefone, dataNascimento, ativo, endereco);
    }

    @Override
    public TipoFuncionario getTipo() {
        return TipoFuncionario.MECANICO;
    }
}
