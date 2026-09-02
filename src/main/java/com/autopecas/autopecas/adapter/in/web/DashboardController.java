package com.autopecas.autopecas.adapter.in.web;

import com.autopecas.autopecas.adapter.in.web.dto.dashboard.DashboardDTO;
import com.autopecas.autopecas.adapter.in.web.dto.dashboard.TempoMedioDTO;
import com.autopecas.autopecas.adapter.in.web.mapper.IndicadoresWebMapper;
import com.autopecas.autopecas.application.port.in.ConsultaDeIndicadores;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Adapter de entrada HTTP do dashboard da oficina. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ConsultaDeIndicadores consultaDeIndicadores;
    private final IndicadoresWebMapper mapper;

    public DashboardController(ConsultaDeIndicadores consultaDeIndicadores,
                               IndicadoresWebMapper mapper) {
        this.consultaDeIndicadores = consultaDeIndicadores;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardDTO> obterDashboard() {
        return ResponseEntity.ok(mapper.paraResposta(consultaDeIndicadores.indicadores()));
    }

    @GetMapping("/tempo-medio-execucao")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TempoMedioDTO>> tempoMedioExecucao() {
        return ResponseEntity.ok(consultaDeIndicadores.tempoMedioDeExecucao().stream()
                .map(mapper::paraResposta).toList());
    }
}
