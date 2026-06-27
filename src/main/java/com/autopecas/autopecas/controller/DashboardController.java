package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.dto.dashboard.DashboardDTO;
import com.autopecas.autopecas.dto.dashboard.TempoMedioDTO;
import com.autopecas.autopecas.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardDTO> obterDashboard() {
        return ResponseEntity.ok(dashboardService.obterDashboard());
    }

    @GetMapping("/tempo-medio-execucao")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TempoMedioDTO>> tempoMedioExecucao() {
        return ResponseEntity.ok(dashboardService.tempoMedioExecucao());
    }
}