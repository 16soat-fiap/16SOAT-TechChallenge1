package com.autopecas.autopecas.adapter.in.web;

import com.autopecas.autopecas.adapter.in.web.dto.servico.ServicoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.servico.ServicoResponseDTO;
import com.autopecas.autopecas.adapter.in.web.mapper.ServicoWebMapper;
import com.autopecas.autopecas.application.port.in.GestaoDeServicos;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Adapter de entrada HTTP do catálogo de serviços. */
@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final GestaoDeServicos gestaoDeServicos;
    private final ServicoWebMapper mapper;

    public ServicoController(GestaoDeServicos gestaoDeServicos, ServicoWebMapper mapper) {
        this.gestaoDeServicos = gestaoDeServicos;
        this.mapper = mapper;
    }

    // MECANICO consulta serviços disponíveis para orçar/executar
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<List<ServicoResponseDTO>> listar() {
        return ResponseEntity.ok(gestaoDeServicos.listarAtivos().stream()
                .map(mapper::paraResposta).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<ServicoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeServicos.porId(id)));
    }

    // Cadastro, edição e desativação — somente ADMIN
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicoResponseDTO> criar(@Valid @RequestBody ServicoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.paraResposta(gestaoDeServicos.cadastrar(mapper.paraComando(dto))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ServicoCreateDTO dto) {
        return ResponseEntity.ok(
                mapper.paraResposta(gestaoDeServicos.atualizar(id, mapper.paraComando(dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        gestaoDeServicos.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
