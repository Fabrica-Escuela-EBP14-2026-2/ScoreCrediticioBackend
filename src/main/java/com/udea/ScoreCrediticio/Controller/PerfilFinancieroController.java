package com.udea.ScoreCrediticio.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.udea.ScoreCrediticio.DTOs.Request.PerfilFinancieroRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.PerfilFinancieroResponseDTO;
import com.udea.ScoreCrediticio.Model.TipoDocumento;
import com.udea.ScoreCrediticio.Services.PerfilFinancieroService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/perfil-financiero")
@Validated
public class PerfilFinancieroController {

    private final PerfilFinancieroService perfilFinancieroService;

    public PerfilFinancieroController(PerfilFinancieroService perfilFinancieroService) {
        this.perfilFinancieroService = perfilFinancieroService;
    }

    @PostMapping
    public ResponseEntity<PerfilFinancieroResponseDTO> registrar(
            @Valid @RequestBody PerfilFinancieroRequestDTO dto) {
        PerfilFinancieroResponseDTO creado = perfilFinancieroService.registrarPerfil(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public ResponseEntity<PerfilFinancieroResponseDTO> consultarPorDocumento(
            @RequestParam TipoDocumento tipoDocumento,
            @RequestParam @NotBlank(message = "El número de documento es obligatorio")
            @Size(max = 20, message = "El número de documento no debe superar los 20 caracteres")
            @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El número de documento tiene un formato inválido")
            String numeroDocumento) {
        PerfilFinancieroResponseDTO ficha =
                perfilFinancieroService.consultarPorDocumento(tipoDocumento, numeroDocumento);
        return ResponseEntity.ok(ficha);
    }
}
