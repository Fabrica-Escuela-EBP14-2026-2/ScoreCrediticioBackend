package com.udea.ScoreCrediticio.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.udea.ScoreCrediticio.DTOs.Request.SolicitanteRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.SolicitanteResponseDTO;
import com.udea.ScoreCrediticio.Services.SolicitanteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/solicitantes")
public class SolicitanteController {

    private final SolicitanteService solicitanteService;

    public SolicitanteController(SolicitanteService solicitanteService) {
        this.solicitanteService = solicitanteService;
    }

    @PostMapping
    public ResponseEntity<SolicitanteResponseDTO> registrar(@Valid @RequestBody SolicitanteRequestDTO dto) {
        SolicitanteResponseDTO creado = solicitanteService.registrarSolicitante(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
