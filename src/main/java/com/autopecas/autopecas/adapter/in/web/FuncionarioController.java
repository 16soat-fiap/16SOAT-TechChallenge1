package com.autopecas.autopecas.adapter.in.web;

import com.autopecas.autopecas.adapter.in.web.dto.funcionario.AtendenteCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.funcionario.FuncionarioResponseDTO;
import com.autopecas.autopecas.adapter.in.web.dto.funcionario.MecanicoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.mapper.FuncionarioWebMapper;
import com.autopecas.autopecas.application.port.in.GestaoDeFuncionarios;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Adapter de entrada HTTP do agregado Funcionario. */
@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    private final GestaoDeFuncionarios gestaoDeFuncionarios;
    private final FuncionarioWebMapper mapper;

    public FuncionarioController(GestaoDeFuncionarios gestaoDeFuncionarios,
                                 FuncionarioWebMapper mapper) {
        this.gestaoDeFuncionarios = gestaoDeFuncionarios;
        this.mapper = mapper;
    }

    // ATENDENTE pode consultar funcionários (ex: escolher mecânico ao atribuir OS)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<FuncionarioResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeFuncionarios.porId(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<List<FuncionarioResponseDTO>> listarFuncionarios() {
        return ResponseEntity.ok(gestaoDeFuncionarios.listarAtivos().stream()
                .map(mapper::paraResposta).toList());
    }

    // Criação e desativação de funcionários — somente ADMIN
    @PostMapping("/mecanico")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuncionarioResponseDTO> criarMecanico(
            @Valid @RequestBody MecanicoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                mapper.paraResposta(gestaoDeFuncionarios.cadastrarMecanico(mapper.paraComando(dto))));
    }

    @PostMapping("/atendente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuncionarioResponseDTO> criarAtendente(
            @Valid @RequestBody AtendenteCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                mapper.paraResposta(gestaoDeFuncionarios.cadastrarAtendente(mapper.paraComando(dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        gestaoDeFuncionarios.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
