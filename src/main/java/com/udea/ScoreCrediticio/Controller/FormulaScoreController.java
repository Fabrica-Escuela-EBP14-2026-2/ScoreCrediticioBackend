package com.udea.ScoreCrediticio.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.udea.ScoreCrediticio.DTOs.Request.DefinirFormulaRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.FormulaResponseDTO;
import com.udea.ScoreCrediticio.Services.FormulaScoreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/formula")
public class FormulaScoreController {

    private final FormulaScoreService formulaScoreService;

    public FormulaScoreController(FormulaScoreService formulaScoreService) {
        this.formulaScoreService = formulaScoreService;
    }

    @PostMapping
    public ResponseEntity<FormulaResponseDTO> guardar(
            @Valid @RequestBody DefinirFormulaRequestDTO dto) {
        FormulaResponseDTO creada = formulaScoreService.guardarFormula(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/vigente")
    public ResponseEntity<FormulaResponseDTO> obtenerVigente() {
        FormulaResponseDTO vigente = formulaScoreService.obtenerFormulaVigente();
        return ResponseEntity.ok(vigente);
    }
}
