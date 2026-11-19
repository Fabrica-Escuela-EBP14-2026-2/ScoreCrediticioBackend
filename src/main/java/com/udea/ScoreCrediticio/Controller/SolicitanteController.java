package com.udea.ScoreCrediticio.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.udea.ScoreCrediticio.DTOs.Request.CalcularRiesgoRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Request.SolicitanteRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.HistorialEvaluacionResponseDTO;
import com.udea.ScoreCrediticio.DTOs.Response.ResultadoRiesgoResponseDTO;
import com.udea.ScoreCrediticio.DTOs.Response.SolicitanteResponseDTO;
import com.udea.ScoreCrediticio.Services.CalculoRiesgoService;
import com.udea.ScoreCrediticio.Services.HistorialEvaluacionService;
import com.udea.ScoreCrediticio.Services.SolicitanteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/solicitantes")
public class SolicitanteController {

    private final SolicitanteService solicitanteService;
    private final CalculoRiesgoService calculoRiesgoService;
    private final HistorialEvaluacionService historialEvaluacionService;

    public SolicitanteController(SolicitanteService solicitanteService,
            CalculoRiesgoService calculoRiesgoService,
            HistorialEvaluacionService historialEvaluacionService) {
        this.solicitanteService = solicitanteService;
        this.calculoRiesgoService = calculoRiesgoService;
        this.historialEvaluacionService = historialEvaluacionService;
    }

    @PostMapping
    public ResponseEntity<SolicitanteResponseDTO> registrar(@Valid @RequestBody SolicitanteRequestDTO dto) {
        SolicitanteResponseDTO creado = solicitanteService.registrarSolicitante(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // HU10: aplica la formula vigente sobre los valores de las variables activas
    // y persiste el resultado (puntaje, nivel y fecha) en el historial del solicitante.
    // HU12: ademas guarda el evaluador y el snapshot de los datos usados en el calculo.
    @PostMapping("/{id}/calcular-riesgo")
    public ResponseEntity<ResultadoRiesgoResponseDTO> calcularRiesgo(
            @PathVariable Long id,
            @Valid @RequestBody CalcularRiesgoRequestDTO dto,
            Authentication authentication) {
        ResultadoRiesgoResponseDTO resultado = calculoRiesgoService.calcularRiesgo(
                id, dto, authentication == null ? null : authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    // HU12: lista las evaluaciones del solicitante de la mas reciente a la mas antigua.
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialEvaluacionResponseDTO>> consultarHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(historialEvaluacionService.consultarHistorial(id));
    }
}
