package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.Funcionario;
import com.autopecas.autopecas.domain.entity.MovimentacaoEstoque;
import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.entity.Peca;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.exception.EstoqueInsuficienteException;
import com.autopecas.autopecas.repository.MovimentacaoEstoqueRepository;
import com.autopecas.autopecas.repository.PecaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final PecaRepository pecaRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Transactional
    public MovimentacaoEstoque registrarMovimentacao(
            Peca peca,
            TipoMovimentacaoEstoque tipo,
            int quantidade,
            String motivo,
            OrdemServico os,
            Funcionario executadoPor) {

        if (tipo == TipoMovimentacaoEstoque.SAIDA) {
            if (!peca.temEstoqueSuficiente(quantidade)) {
                throw new EstoqueInsuficienteException(
                        peca.getNome(), peca.getQuantidadeEstoque(), quantidade
                );
            }
            peca.decrementarEstoque(quantidade);
        } else {
            peca.incrementarEstoque(quantidade);
        }

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .peca(peca)
                .tipo(tipo)
                .quantidade(quantidade)
                .saldoApos(peca.getQuantidadeEstoque())
                .motivo(motivo)
                .ordemServico(os)
                .executadoPor(executadoPor)
                .build();

        MovimentacaoEstoque salva = movimentacaoEstoqueRepository.save(movimentacao);
        log.info("Movimentação de estoque registrada. Peça: {}, Tipo: {}, Quantidade: {}, Saldo após: {}",
                peca.getCodigo(), tipo, quantidade, peca.getQuantidadeEstoque());
        return salva;
    }
}
