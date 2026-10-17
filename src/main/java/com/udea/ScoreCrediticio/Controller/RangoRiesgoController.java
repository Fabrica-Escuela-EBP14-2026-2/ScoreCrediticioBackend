package com.udea.ScoreCrediticio.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.udea.ScoreCrediticio.DTOs.Request.DefinirRangosRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.ClasificacionRiesgoResponseDTO;
import com.udea.ScoreCrediticio.DTOs.Response.EscalaRiesgoResponseDTO;
import com.udea.ScoreCrediticio.Services.RangoRiesgoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rangos")
public class RangoRiesgoController {

    private final RangoRiesgoService rangoRiesgoService;

    public RangoRiesgoController(RangoRiesgoService rangoRiesgoService) {
        this.rangoRiesgoService = rangoRiesgoService;
    }

    @PostMapping
    public ResponseEntity<EscalaRiesgoResponseDTO> guardar(
            @Valid @RequestBody DefinirRangosRequestDTO dto) {
        EscalaRiesgoResponseDTO guardada = rangoRiesgoService.guardarRangos(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @GetMapping
    public ResponseEntity<EscalaRiesgoResponseDTO> obtener() {
        return ResponseEntity.ok(rangoRiesgoService.obtenerEscala());
    }

    // Clasifica un puntaje (0-1000) en el nivel de riesgo de la escala vigente.
    @GetMapping("/clasificar")
    public ResponseEntity<ClasificacionRiesgoResponseDTO> clasificar(
            @RequestParam int puntaje) {
        return ResponseEntity.ok(rangoRiesgoService.clasificar(puntaje));
    }
}
