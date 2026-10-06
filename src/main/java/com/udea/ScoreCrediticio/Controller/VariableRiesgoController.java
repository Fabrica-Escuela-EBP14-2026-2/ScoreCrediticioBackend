package com.udea.ScoreCrediticio.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.udea.ScoreCrediticio.DTOs.Request.VariableEstadoRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Request.VariableRiesgoRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.VariableRiesgoResponseDTO;
import com.udea.ScoreCrediticio.Services.VariableRiesgoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/variables")
public class VariableRiesgoController {

    private final VariableRiesgoService variableRiesgoService;

    public VariableRiesgoController(VariableRiesgoService variableRiesgoService) {
        this.variableRiesgoService = variableRiesgoService;
    }

    @PostMapping
    public ResponseEntity<VariableRiesgoResponseDTO> registrar(
            @Valid @RequestBody VariableRiesgoRequestDTO dto) {
        VariableRiesgoResponseDTO creada = variableRiesgoService.registrarVariable(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    public ResponseEntity<List<VariableRiesgoResponseDTO>> listar() {
        return ResponseEntity.ok(variableRiesgoService.listarVariables());
    }

    // Activa o inactiva una variable sin borrarla: la fila sigue en el catalogo y el
    // listado la muestra con estado "Inactiva", pero deja de sumar su peso al modelo.
    @PatchMapping("/{id}/estado")
    public ResponseEntity<VariableRiesgoResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody VariableEstadoRequestDTO dto) {
        VariableRiesgoResponseDTO actualizada = variableRiesgoService.cambiarEstado(id, dto.getEstado());
        return ResponseEntity.ok(actualizada);
    }
}
