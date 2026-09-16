package com.udea.ScoreCrediticio.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.udea.ScoreCrediticio.DTOs.Request.PerfilFinancieroRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.PerfilFinancieroResponseDTO;
import com.udea.ScoreCrediticio.Services.PerfilFinancieroService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/solicitantes/{solicitanteId}/perfil-financiero")
public class PerfilFinancieroController {

    private final PerfilFinancieroService perfilFinancieroService;

    public PerfilFinancieroController(PerfilFinancieroService perfilFinancieroService) {
        this.perfilFinancieroService = perfilFinancieroService;
    }

    @PostMapping
    public ResponseEntity<PerfilFinancieroResponseDTO> registrar(
            @PathVariable Long solicitanteId,
            @Valid @RequestBody PerfilFinancieroRequestDTO dto) {
        PerfilFinancieroResponseDTO creado = perfilFinancieroService.registrarPerfil(solicitanteId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
