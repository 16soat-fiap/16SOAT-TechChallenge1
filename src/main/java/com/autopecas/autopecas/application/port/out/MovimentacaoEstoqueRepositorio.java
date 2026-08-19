package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.domain.model.estoque.MovimentacaoEstoque;

import java.util.List;
import java.util.UUID;

/** Port de saída do livro de movimentações de estoque, que é somente-inserção. */
public interface MovimentacaoEstoqueRepositorio {

    MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacao);

    List<MovimentacaoEstoque> daPeca(UUID pecaId);

    List<MovimentacaoEstoque> daOrdemServico(UUID ordemServicoId);
}
