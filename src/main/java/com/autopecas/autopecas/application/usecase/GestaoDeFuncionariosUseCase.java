package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.port.in.GestaoDeFuncionarios;
import com.autopecas.autopecas.application.port.in.view.FuncionarioView;
import com.autopecas.autopecas.application.port.out.FuncionarioRepositorio;
import com.autopecas.autopecas.application.port.out.GeradorMatricula;
import com.autopecas.autopecas.application.port.out.Transacao;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.funcionario.Atendente;
import com.autopecas.autopecas.domain.model.funcionario.Funcionario;
import com.autopecas.autopecas.domain.model.funcionario.Mecanico;

import java.util.List;
import java.util.UUID;

/** Casos de uso do agregado Funcionario. */
public class GestaoDeFuncionariosUseCase implements GestaoDeFuncionarios {

    private final FuncionarioRepositorio funcionarioRepositorio;
    private final GeradorMatricula geradorMatricula;
    private final Transacao transacao;

    public GestaoDeFuncionariosUseCase(FuncionarioRepositorio funcionarioRepositorio,
                                       GeradorMatricula geradorMatricula,
                                       Transacao transacao) {
        this.funcionarioRepositorio = funcionarioRepositorio;
        this.geradorMatricula = geradorMatricula;
        this.transacao = transacao;
    }

    @Override
    public List<FuncionarioView> listarAtivos() {
        return funcionarioRepositorio.ativos().stream().map(this::paraView).toList();
    }

    @Override
    public FuncionarioView porId(UUID id) {
        return paraView(buscar(id, "Funcionário não encontrado, id: " + id));
    }

    @Override
    public FuncionarioView cadastrarMecanico(Cadastrar comando) {
        return transacao.executar(() -> {
            validarCpfUnico(comando.cpf());
            Mecanico mecanico = Mecanico.criar(geradorMatricula.proximaDeMecanico(), comando.cpf(),
                    comando.nome(), comando.email(), comando.telefone(), comando.dataNascimento());
            return paraView(funcionarioRepositorio.salvar(mecanico));
        });
    }

    @Override
    public FuncionarioView cadastrarAtendente(Cadastrar comando) {
        return transacao.executar(() -> {
            validarCpfUnico(comando.cpf());
            Atendente atendente = Atendente.criar(geradorMatricula.proximaDeAtendente(), comando.cpf(),
                    comando.nome(), comando.email(), comando.telefone(), comando.dataNascimento());
            return paraView(funcionarioRepositorio.salvar(atendente));
        });
    }

    @Override
    public void desativar(UUID id) {
        transacao.executar(() -> {
            Funcionario funcionario = buscar(id, "Funcionário não encontrado com ID: " + id);
            funcionario.desativar();
            funcionarioRepositorio.salvar(funcionario);
        });
    }

    private Funcionario buscar(UUID id, String mensagemSeAusente) {
        return funcionarioRepositorio.porId(id)
                .orElseThrow(() -> new ResourceNotFoundException(mensagemSeAusente));
    }

    private void validarCpfUnico(String cpf) {
        if (funcionarioRepositorio.existePorCpf(cpf)) {
            throw new BusinessException("CPF: " + cpf + " , já cadastrado");
        }
    }

    private FuncionarioView paraView(Funcionario funcionario) {
        return new FuncionarioView(funcionario.getId(), funcionario.getMatricula(), funcionario.getNome(),
                funcionario.getEmail(), funcionario.getTelefone(), funcionario.getTipo().name(),
                funcionario.isAtivo());
    }
}
