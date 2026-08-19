package com.autopecas.autopecas.application.port.in;

import com.autopecas.autopecas.application.port.in.view.MovimentacaoView;
import com.autopecas.autopecas.application.port.in.view.PecaView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Inbound port do agregado Peca, incluindo as movimentações manuais de estoque. */
public interface GestaoDePecas {

    /** Lista as peças ativas ou, se estoqueBaixo for verdadeiro, só as que estão no limite. */
    List<PecaView> listar(boolean apenasEstoqueBaixo);

    PecaView porId(UUID id);

    PecaView porCodigo(String codigo);

    PecaView cadastrar(Cadastrar comando);

    PecaView atualizar(UUID id, AtualizarDados comando);

    void desativar(UUID id);

    MovimentacaoView registrarMovimentacao(UUID pecaId, RegistrarMovimentacao comando);

    record Cadastrar(
            String codigo,
            String nome,
            String descricao,
            BigDecimal precoVenda,
            Integer quantidadeInicial,
            Integer quantidadeMinima,
            String unidade
    ) {
    }

    /** Atualização parcial: campos nulos são ignorados. */
    record AtualizarDados(
            String nome,
            String descricao,
            BigDecimal precoVenda,
            Integer quantidadeMinima,
            String unidade
    ) {
    }

    record RegistrarMovimentacao(
            String tipo,
            Integer quantidade,
            String motivo
    ) {
    }
}
