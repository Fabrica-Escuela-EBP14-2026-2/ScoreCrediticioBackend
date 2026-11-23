package com.udea.ScoreCrediticio.Controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.udea.ScoreCrediticio.DTOs.Response.DistribucionRiesgoResponseDTO;
import com.udea.ScoreCrediticio.Services.DashboardRiesgoService;

/**
 * HU13: expone los indicadores de distribucion de riesgo de cartera para el
 * Administrador de Riesgos.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardRiesgoService dashboardRiesgoService;

    public DashboardController(DashboardRiesgoService dashboardRiesgoService) {
        this.dashboardRiesgoService = dashboardRiesgoService;
    }

    @GetMapping("/distribucion-riesgo")
    public ResponseEntity<DistribucionRiesgoResponseDTO> obtenerDistribucionRiesgo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(dashboardRiesgoService.obtenerDistribucion(fechaInicio, fechaFin));
    }
}
