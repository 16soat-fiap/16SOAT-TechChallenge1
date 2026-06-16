package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.dto.funcionario.AtendenteCreateDTO;
import com.autopecas.autopecas.dto.funcionario.FuncionarioResponseDTO;
import com.autopecas.autopecas.dto.funcionario.MecanicoCreateDTO;
import com.autopecas.autopecas.service.FuncionarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> buscarPorId(@PathVariable UUID id){
        return ResponseEntity.ok(funcionarioService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<FuncionarioResponseDTO>> listarFuncionarios(){
        return ResponseEntity.ok(funcionarioService.listar());
    }

    @PostMapping("/mecanico")
    public ResponseEntity<FuncionarioResponseDTO> criarMecanico(@Valid @RequestBody MecanicoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.criarMecanico(dto));
    }

    @PostMapping("/atendente")
    public ResponseEntity<FuncionarioResponseDTO> criarAtendente(@Valid @RequestBody AtendenteCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.criarAtendente(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        funcionarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }

}
