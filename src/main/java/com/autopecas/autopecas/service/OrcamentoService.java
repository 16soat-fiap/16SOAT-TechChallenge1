package com.autopecas.autopecas.service;

import com.autopecas.autopecas.domain.entity.*;
import com.autopecas.autopecas.domain.enums.StatusOS;
import com.autopecas.autopecas.domain.enums.StatusOrcamento;
import com.autopecas.autopecas.domain.enums.TipoMovimentacaoEstoque;
import com.autopecas.autopecas.dto.orcamento.AprovarRejeitarDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoCreateDTO;
import com.autopecas.autopecas.dto.orcamento.OrcamentoResponseDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.mapper.OrcamentoMapper;
import com.autopecas.autopecas.repository.OrcamentoRepository;
import com.autopecas.autopecas.repository.OrdemServicoRepository;
import com.autopecas.autopecas.repository.PecaRepository;
import com.autopecas.autopecas.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final ServicoRepository servicoRepository;
    private final PecaRepository pecaRepository;
    private final EstoqueService estoqueService;
    private final OrcamentoMapper orcamentoMapper;

    @Transactional(readOnly = true)
    public List<OrcamentoResponseDTO> listar(UUID osId) {
        if (!ordemServicoRepository.existsById(osId)) {
            throw new ResourceNotFoundException("Ordem de serviço não encontrada com ID: " + osId);
        }
        return orcamentoRepository.findById(osId)
                .stream()
                .map(orcamentoMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrcamentoResponseDTO criarOrcamento(UUID osId, OrcamentoCreateDTO dto){
        OrdemServico os = ordemServicoRepository.findById(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada"));

        if (orcamentoRepository.existsByOrdemServicoIdAndStatus(osId, StatusOrcamento.APROVADA)){
            throw new BusinessException("Já existe um orçamento aprovado para esta OS");
        }

        Orcamento orcamento = Orcamento.builder()
                .condicoesPagamento(dto.condicoesPagamento())
                .prazoExecucaoDias(dto.prazoExecucaoDias())
                .dataValidade(dto.dataValidade())
                .observacoes(dto.observacoes())
                .build();

        os.adicionarOrcamento(orcamento);

        // Adicionar itens de serviço
        if (dto.itensServico() != null) {
            for (OrcamentoCreateDTO.ItemServicoDTO itemDto : dto.itensServico()) {
                Servico servico = servicoRepository.findById(itemDto.servicoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + itemDto.servicoId()));

                ItemOrcamentoServico item = ItemOrcamentoServico.builder()
                        .orcamento(orcamento)
                        .servico(servico)
                        .quantidade(itemDto.quantidade() != null ? itemDto.quantidade() : 1)
                        .precoUnitario(servico.getPrecoBase())
                        .build();
                orcamento.getItensServico().add(item);
            }
        }

        // Adicionar itens de peça
        if (dto.itensPeca() != null) {
            for (OrcamentoCreateDTO.ItemPecaDTO itemDto : dto.itensPeca()) {
                Peca peca = pecaRepository.findById(itemDto.pecaId())
                        .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada com ID: " + itemDto.pecaId()));

                ItemOrcamentoPeca item = ItemOrcamentoPeca.builder()
                        .orcamento(orcamento)
                        .peca(peca)
                        .quantidade(itemDto.quantidade() != null ? itemDto.quantidade() : 1)
                        .precoUnitario(peca.getPrecoVenda())
                        .build();
                orcamento.getItensPeca().add(item);
            }
        }

        orcamento.recalcular();
        ordemServicoRepository.save(os);

        log.info("Orçamento criado para OS {}. Versão: {}", osId, orcamento.getVersao());
        return orcamentoMapper.toResponse(orcamento);
    }

    @Transactional
    public OrcamentoResponseDTO enviar(UUID osId, UUID orcamentoId) {
        Orcamento orcamento = buscarOrcamentoDaOS(osId, orcamentoId);
        try {
            orcamento.enviar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        Orcamento salvo = orcamentoRepository.save(orcamento);
        log.info("Orçamento {} enviado para OS {}.", orcamentoId, osId);
        return orcamentoMapper.toResponse(salvo);
    }

    @Transactional
    public OrcamentoResponseDTO aprovar(UUID osId, UUID orcamentoId) {
        Orcamento orcamento = buscarOrcamentoDaOS(osId, orcamentoId);

        if (orcamentoRepository.existsByOrdemServicoIdAndStatus(osId, StatusOrcamento.APROVADA)) {
            throw new BusinessException("Já existe um orçamento aprovado para esta OS.");
        }

        try {
            orcamento.aprovar();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        OrdemServico os = orcamento.getOrdemServico();

        // Avança a OS para EM_EXECUCAO
        try {
            os.avancarStatus(StatusOS.EM_EXECUCAO);
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }

        os.setValorTotalAprovado(orcamento.getValorTotal());

        // Copiar itens de serviço do orçamento para a OS
        List<ItemServicoOS> itensServicoOS = new ArrayList<>();
        for (ItemOrcamentoServico itemOrc : orcamento.getItensServico()) {
            ItemServicoOS itemOS = ItemServicoOS.builder()
                    .servico(itemOrc.getServico())
                    .quantidade(itemOrc.getQuantidade())
                    .precoUnitario(itemOrc.getPrecoUnitario())
                    .descontoUnitario(itemOrc.getDescontoUnitario())
                    .build();
            os.adicionarItemServico(itemOS);
        }

        // Copiar itens de peça e decrementar estoque
        for (ItemOrcamentoPeca itemOrc : orcamento.getItensPeca()) {
            ItemPecaOS itemOS = ItemPecaOS.builder()
                    .peca(itemOrc.getPeca())
                    .quantidade(itemOrc.getQuantidade())
                    .precoUnitario(itemOrc.getPrecoUnitario())
                    .descontoUnitario(itemOrc.getDescontoUnitario())
                    .build();
            os.adicionarItemPeca(itemOS);

            // Decrementar estoque
            estoqueService.registrarMovimentacao(
                    itemOrc.getPeca(),
                    TipoMovimentacaoEstoque.SAIDA,
                    itemOrc.getQuantidade(),
                    "Saída por aprovação do orçamento da OS " + os.getNumero(),
                    os,
                    null
            );
        }

        orcamentoRepository.save(orcamento);
        ordemServicoRepository.save(os);

        log.info("Orçamento {} aprovado para OS {}. OS avançada para EM_EXECUCAO.", orcamentoId, osId);
        return orcamentoMapper.toResponse(orcamento);
    }

    @Transactional
    public OrcamentoResponseDTO rejeitar(UUID osId, UUID orcamentoId, AprovarRejeitarDTO dto) {
        Orcamento orcamento = buscarOrcamentoDaOS(osId, orcamentoId);
        try {
            orcamento.rejeitar(dto.motivo());
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage());
        }
        Orcamento salvo = orcamentoRepository.save(orcamento);
        log.info("Orçamento {} rejeitado para OS {}.", orcamentoId, osId);
        return orcamentoMapper.toResponse(salvo);
    }

    private Orcamento buscarOrcamentoDaOS(UUID osId, UUID orcamentoId){
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Orcamento não encontrado"));
        if (!orcamento.getOrdemServico().getId().equals(osId)) {
            throw new BusinessException("O orçamento não pertence à OS informada.");
        }
        return orcamento;
    }
}
