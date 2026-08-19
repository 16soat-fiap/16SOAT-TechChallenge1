package com.autopecas.autopecas.adapter.in.web;

import com.autopecas.autopecas.adapter.in.web.dto.peca.MovimentacaoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.peca.MovimentacaoResponseDTO;
import com.autopecas.autopecas.adapter.in.web.dto.peca.PecaCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.peca.PecaResponseDTO;
import com.autopecas.autopecas.adapter.in.web.mapper.PecaWebMapper;
import com.autopecas.autopecas.application.port.in.GestaoDePecas;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Adapter de entrada HTTP do agregado Peca e das movimentações de estoque. */
@RestController
@RequestMapping("/api/pecas")
public class PecaController {

    private final GestaoDePecas gestaoDePecas;
    private final PecaWebMapper mapper;

    public PecaController(GestaoDePecas gestaoDePecas, PecaWebMapper mapper) {
        this.gestaoDePecas = gestaoDePecas;
        this.mapper = mapper;
    }

    // MECANICO consulta peças disponíveis para incluir em orçamentos/OS
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<List<PecaResponseDTO>> listar(
            @RequestParam(required = false) Boolean estoqueBaixo) {
        return ResponseEntity.ok(gestaoDePecas.listar(Boolean.TRUE.equals(estoqueBaixo)).stream()
                .map(mapper::paraResposta).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDePecas.porId(id)));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<PecaResponseDTO> buscarPorCodigo(@RequestParam String codigo) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDePecas.porCodigo(codigo)));
    }

    // Cadastro e edição de peças — somente ADMIN
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PecaResponseDTO> criar(@Valid @RequestBody PecaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.paraResposta(gestaoDePecas.cadastrar(mapper.paraComando(dto))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PecaResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PecaCreateDTO dto) {
        return ResponseEntity.ok(mapper.paraResposta(
                gestaoDePecas.atualizar(id, mapper.paraComandoDeAtualizacao(dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        gestaoDePecas.desativar(id);
        return ResponseEntity.noContent().build();
    }

    // Movimentação de estoque — ATENDENTE registra entradas/saídas manuais; ADMIN tem acesso total
    @PostMapping("/{id}/movimentacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<MovimentacaoResponseDTO> registrarMovimentacao(
            @PathVariable UUID id,
            @Valid @RequestBody MovimentacaoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.paraResposta(
                gestaoDePecas.registrarMovimentacao(id, mapper.paraComando(dto))));
    }
}
