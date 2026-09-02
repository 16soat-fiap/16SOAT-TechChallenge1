package com.autopecas.autopecas.adapter.in.web;

import com.autopecas.autopecas.adapter.in.web.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.adapter.in.web.dto.cliente.ClienteCreatePJDTO;
import com.autopecas.autopecas.adapter.in.web.dto.cliente.ClienteResponseDTO;
import com.autopecas.autopecas.adapter.in.web.dto.cliente.ClienteUpdateDTO;
import com.autopecas.autopecas.adapter.in.web.mapper.ClienteWebMapper;
import com.autopecas.autopecas.application.port.in.GestaoDeClientes;
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

/** Adapter de entrada HTTP do agregado Cliente. Apenas traduz requisições em chamadas de port. */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final GestaoDeClientes gestaoDeClientes;
    private final ClienteWebMapper mapper;

    public ClienteController(GestaoDeClientes gestaoDeClientes, ClienteWebMapper mapper) {
        this.gestaoDeClientes = gestaoDeClientes;
        this.mapper = mapper;
    }

    // CLIENTE não pode listar todos — apenas consultar o próprio cadastro pelo ID
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<List<ClienteResponseDTO>> listar() {
        return ResponseEntity.ok(gestaoDeClientes.listarAtivos().stream()
                .map(mapper::paraResposta).toList());
    }

    // CLIENTE pode consultar o próprio cadastro — e somente o próprio
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE') "
            + "or @propriedade.ehOProprioCliente(authentication, #id)")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeClientes.porId(id)));
    }

    // Busca por documento — uso interno da oficina
    @GetMapping("/buscarDOC")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<ClienteResponseDTO> buscarPorDocumento(@RequestParam String documento) {
        return ResponseEntity.ok(mapper.paraResposta(gestaoDeClientes.porDocumento(documento)));
    }

    @PostMapping("/pf")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<ClienteResponseDTO> criarPF(@Valid @RequestBody ClienteCreatePFDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.paraResposta(gestaoDeClientes.cadastrarPF(mapper.paraComando(dto))));
    }

    @PostMapping("/pj")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<ClienteResponseDTO> criarPJ(@Valid @RequestBody ClienteCreatePJDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.paraResposta(gestaoDeClientes.cadastrarPJ(mapper.paraComando(dto))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    public ResponseEntity<ClienteResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ClienteUpdateDTO dto) {
        return ResponseEntity.ok(
                mapper.paraResposta(gestaoDeClientes.atualizar(id, mapper.paraComando(dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        gestaoDeClientes.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
