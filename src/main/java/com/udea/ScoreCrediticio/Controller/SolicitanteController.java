package com.udea.ScoreCrediticio.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.udea.ScoreCrediticio.DTOs.Request.CalcularRiesgoRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Request.SolicitanteRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.ResultadoRiesgoResponseDTO;
import com.udea.ScoreCrediticio.DTOs.Response.SolicitanteResponseDTO;
import com.udea.ScoreCrediticio.Services.CalculoRiesgoService;
import com.udea.ScoreCrediticio.Services.SolicitanteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/solicitantes")
public class SolicitanteController {

    private final SolicitanteService solicitanteService;
    private final CalculoRiesgoService calculoRiesgoService;

    public SolicitanteController(SolicitanteService solicitanteService,
            CalculoRiesgoService calculoRiesgoService) {
        this.solicitanteService = solicitanteService;
        this.calculoRiesgoService = calculoRiesgoService;
    }

    @PostMapping
    public ResponseEntity<SolicitanteResponseDTO> registrar(@Valid @RequestBody SolicitanteRequestDTO dto) {
        SolicitanteResponseDTO creado = solicitanteService.registrarSolicitante(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // HU10: aplica la formula vigente sobre los valores de las variables activas
    // y persiste el resultado (puntaje, nivel y fecha) en el historial del solicitante.
    @PostMapping("/{id}/calcular-riesgo")
    public ResponseEntity<ResultadoRiesgoResponseDTO> calcularRiesgo(
            @PathVariable Long id,
            @Valid @RequestBody CalcularRiesgoRequestDTO dto) {
        ResultadoRiesgoResponseDTO resultado = calculoRiesgoService.calcularRiesgo(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
