package com.autopecas.autopecas.application.usecase;

import com.autopecas.autopecas.application.port.in.GestaoDePecas;
import com.autopecas.autopecas.application.port.in.view.MovimentacaoView;
import com.autopecas.autopecas.application.port.in.view.PecaView;
import com.autopecas.autopecas.application.port.out.MovimentacaoEstoqueRepositorio;
import com.autopecas.autopecas.application.port.out.PecaRepositorio;
import com.autopecas.autopecas.application.port.out.Transacao;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.exception.ResourceNotFoundException;
import com.autopecas.autopecas.domain.model.estoque.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.model.estoque.Peca;
import com.autopecas.autopecas.domain.service.MovimentadorDeEstoque;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso do agregado Peca.
 *
 * <p>Toda movimentação de estoque grava explicitamente a peça alterada e o registro de
 * movimentação. Na versão anterior ao refactor a peça não era salva: o saldo persistia apenas
 * porque a entidade estava gerenciada pelo Hibernate (dirty checking). Com o domínio
 * desacoplado da persistência, esse efeito colateral implícito não existe mais.
 */
public class GestaoDePecasUseCase implements GestaoDePecas {

    private static final int QUANTIDADE_MINIMA_PADRAO = 1;
    private static final String UNIDADE_PADRAO = "un";

    private final PecaRepositorio pecaRepositorio;
    private final MovimentacaoEstoqueRepositorio movimentacaoRepositorio;
    private final MovimentadorDeEstoque movimentadorDeEstoque;
    private final Transacao transacao;

    public GestaoDePecasUseCase(PecaRepositorio pecaRepositorio,
                                MovimentacaoEstoqueRepositorio movimentacaoRepositorio,
                                MovimentadorDeEstoque movimentadorDeEstoque,
                                Transacao transacao) {
        this.pecaRepositorio = pecaRepositorio;
        this.movimentacaoRepositorio = movimentacaoRepositorio;
        this.movimentadorDeEstoque = movimentadorDeEstoque;
        this.transacao = transacao;
    }

    @Override
    public List<PecaView> listar(boolean apenasEstoqueBaixo) {
        List<Peca> pecas = apenasEstoqueBaixo
                ? pecaRepositorio.comEstoqueBaixo()
                : pecaRepositorio.ativas();
        return pecas.stream().map(this::paraView).toList();
    }

    @Override
    public PecaView porId(UUID id) {
        return paraView(buscar(id, "Id " + id + " não encontrado"));
    }

    @Override
    public PecaView porCodigo(String codigo) {
        Peca peca = pecaRepositorio.porCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Peca " + codigo + " não encontrada"));
        return paraView(peca);
    }

    @Override
    public PecaView cadastrar(Cadastrar comando) {
        return transacao.executar(() -> {
            if (pecaRepositorio.existePorCodigo(comando.codigo())) {
                throw new BusinessException("Código da peça já está cadastrado: " + comando.codigo());
            }

            int quantidadeInicial = valorOuPadrao(comando.quantidadeInicial(), 0);
            int quantidadeMinima = valorOuPadrao(comando.quantidadeMinima(), QUANTIDADE_MINIMA_PADRAO);
            String unidade = comando.unidade() != null ? comando.unidade() : UNIDADE_PADRAO;

            Peca peca = Peca.criar(comando.codigo(), comando.nome(), comando.descricao(),
                    comando.precoVenda(), quantidadeMinima, unidade);
            Peca salva = pecaRepositorio.salvar(peca);

            if (quantidadeInicial > 0) {
                MovimentacaoEstoque movimentacao = movimentadorDeEstoque.movimentar(salva,
                        TipoMovimentacaoEstoque.ENTRADA, quantidadeInicial,
                        "Estoque inicial na criação da peça", null, null);
                movimentacaoRepositorio.salvar(movimentacao);
                salva = pecaRepositorio.salvar(salva);
            }

            return paraView(salva);
        });
    }

    @Override
    public PecaView atualizar(UUID id, AtualizarDados comando) {
        return transacao.executar(() -> {
            Peca peca = buscar(id, "Peça não encontrada");
            peca.atualizarDados(comando.nome(), comando.descricao(), comando.precoVenda(),
                    comando.quantidadeMinima(), comando.unidade());
            return paraView(pecaRepositorio.salvar(peca));
        });
    }

    @Override
    public void desativar(UUID id) {
        transacao.executar(() -> {
            Peca peca = buscar(id, "Peça não encontrada. ID: " + id);
            peca.desativar();
            pecaRepositorio.salvar(peca);
        });
    }

    @Override
    public MovimentacaoView registrarMovimentacao(UUID pecaId, RegistrarMovimentacao comando) {
        TipoMovimentacaoEstoque tipo = converterTipo(comando.tipo());

        return transacao.executar(() -> {
            Peca peca = buscar(pecaId, "Peça não encontrada. ID: " + pecaId);

            MovimentacaoEstoque movimentacao = movimentadorDeEstoque.movimentar(peca, tipo,
                    comando.quantidade(), comando.motivo(), null, null);

            MovimentacaoEstoque salva = movimentacaoRepositorio.salvar(movimentacao);
            pecaRepositorio.salvar(peca);

            return new MovimentacaoView(salva.getId(), salva.getTipo().name(), salva.getQuantidade(),
                    salva.getSaldoApos(), salva.getMotivo(), salva.getCriadoEm());
        });
    }

    private Peca buscar(UUID id, String mensagemSeAusente) {
        return pecaRepositorio.porId(id)
                .orElseThrow(() -> new ResourceNotFoundException(mensagemSeAusente));
    }

    private TipoMovimentacaoEstoque converterTipo(String tipo) {
        try {
            return TipoMovimentacaoEstoque.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tipo de movimentação inválido: " + tipo);
        }
    }

    private int valorOuPadrao(Integer valor, int padrao) {
        return valor != null ? valor : padrao;
    }

    private PecaView paraView(Peca peca) {
        return new PecaView(peca.getId(), peca.getCodigo(), peca.getNome(), peca.getDescricao(),
                peca.getMarca(), peca.getPrecoVenda(), peca.getQuantidadeEstoque(),
                peca.getQuantidadeMinima(), peca.getUnidade(), peca.isAtivo(), peca.getCriadoEm(),
                peca.getAtualizadoEm(), peca.estoqueBaixo());
    }
}
