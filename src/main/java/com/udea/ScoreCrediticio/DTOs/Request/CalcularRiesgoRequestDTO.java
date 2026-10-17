package com.udea.ScoreCrediticio.DTOs.Request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * Datos requeridos por el boton "Calcular Riesgo" (HU10): un valor 0-100 por
 * cada variable de la formula vigente.
 */
public class CalcularRiesgoRequestDTO {

    @NotEmpty(message = "La lista de valores de las variables no puede estar vacia")
    @Valid
    private List<ValorVariableRequestDTO> valores;

    public CalcularRiesgoRequestDTO() {
    }

    public CalcularRiesgoRequestDTO(List<ValorVariableRequestDTO> valores) {
        this.valores = valores;
    }

    public List<ValorVariableRequestDTO> getValores() {
        return valores;
    }

    public void setValores(List<ValorVariableRequestDTO> valores) {
        this.valores = valores;
    }
}
