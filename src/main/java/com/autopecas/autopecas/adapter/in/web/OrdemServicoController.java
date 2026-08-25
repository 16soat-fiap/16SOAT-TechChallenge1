package com.autopecas.autopecas.adapter.in.web;

import com.autopecas.autopecas.adapter.in.web.dto.os.AtribuirMecanicoDTO;
import com.autopecas.autopecas.adapter.in.web.dto.os.AvancarStatusDTO;
import com.autopecas.autopecas.adapter.in.web.dto.os.DiagnosticoDTO;
import com.autopecas.autopecas.adapter.in.web.dto.os.OrdemServicoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.os.OrdemServicoResponseDTO;
import com.autopecas.autopecas.adapter.in.web.mapper.OrdemServicoWebMapper;
import com.autopecas.autopecas.adapter.in.web.mapper.PaginacaoWebMapper;
import com.autopecas.autopecas.application.port.in.GestaoDeOrdensServico;
import com.autopecas.autopecas.domain.enums.StatusOS;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Adapter de entrada HTTP do agregado OrdemServico.
 *
 * <p>O e-mail do usuário autenticado é extraído aqui e entra no command — a aplicação não
 * conhece Authentication nem qualquer tipo do Spring Security.
 */
@RestController
@RequestMapping("/api/ordens-servico")
public class OrdemServicoController {

    private final GestaoDeOrdensServico gestaoDeOrdensServico;
    private final OrdemServicoWebMapper mapper;
    private final PaginacaoWebMapper paginacaoMapper;

    public OrdemServicoController(GestaoDeOrdensServico gestaoDeOrdensServico,
                                  OrdemServicoWebMapper mapper,
                                  PaginacaoWebMapper paginacaoMapper) {
        this.gestaoDeOrdensServico = gestaoDeOrdensServico;
        this.mapper = mapper;
        this.paginacaoMapper = paginacaoMapper;
    }

    // O CLIENTE só enxerga a listagem filtrada pelo próprio clienteId
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO') "
            + "or @propriedade.ehOProprioCliente(authentication, #clienteId)")
    public ResponseEntity<Page<OrdemServicoResponseDTO>> listar(
            @RequestParam(required = false) StatusOS status,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) UUID mecanicoId,
            @PageableDefault(size = 20) Pageable pageable) {
        var pagina = gestaoDeOrdensServico.listar(status, clienteId, mecanicoId,
                paginacaoMapper.paraRequisicao(pageable));
        return ResponseEntity.ok(paginacaoMapper.paraPage(pagina, pageable, mapper::paraResposta));
    }

    @GetMapping("/{numero}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO') "
            + "or @propriedade.ehDonoDaOrdemServicoPorNumero(authentication, #numero)")
    public ResponseEntity<OrdemServicoResponseDTO> buscarPorNumero(@PathVariable String numero) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeOrdensServico.porNumero(numero)));
    }

    // Abrir OS é ato de recepção — quem recebe o veículo é o atendente
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<OrdemServicoResponseDTO> criar(
            @Valid @RequestBody OrdemServicoCreateDTO dto,
            Authentication authentication) {
        var comando = new GestaoDeOrdensServico.Abrir(dto.clienteId(), dto.veiculoId(),
                dto.queixaCliente(), dto.observacoesEntrada(), dto.quilometragemEntrada(),
                emailDe(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.paraResposta(gestaoDeOrdensServico.abrir(comando)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<OrdemServicoResponseDTO> avancarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AvancarStatusDTO dto,
            Authentication authentication) {
        var comando = new GestaoDeOrdensServico.AvancarStatus(dto.novoStatus(), dto.observacao(),
                emailDe(authentication));
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeOrdensServico.avancarStatus(id, comando)));
    }

    // O diagnóstico é técnico: quem registra é o mecânico
    @PatchMapping("/{id}/diagnostico")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICO')")
    public ResponseEntity<OrdemServicoResponseDTO> registrarDiagnostico(
            @PathVariable UUID id,
            @Valid @RequestBody DiagnosticoDTO dto) {
        return ResponseEntity.ok(mapper.paraResposta(
                gestaoDeOrdensServico.registrarDiagnostico(id, dto.diagnostico())));
    }

    @PatchMapping("/{id}/mecanico")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<OrdemServicoResponseDTO> atribuirMecanico(
            @PathVariable UUID id,
            @Valid @RequestBody AtribuirMecanicoDTO dto) {
        return ResponseEntity.ok(mapper.paraResposta(
                gestaoDeOrdensServico.atribuirMecanico(id, dto.mecanicoId())));
    }

    private String emailDe(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
