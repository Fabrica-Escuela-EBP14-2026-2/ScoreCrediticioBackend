package com.udea.ScoreCrediticio.DTOs.Request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * Escala completa de niveles de riesgo enviada por el Administrador.
 * Debe contener exactamente un rango por cada nivel (Bajo, Medio, Alto).
 */
public class DefinirRangosRequestDTO {

    @NotEmpty(message = "La lista de rangos no puede estar vacia")
    @Valid
    private List<RangoItemRequestDTO> rangos;

    public DefinirRangosRequestDTO() {
    }

    public DefinirRangosRequestDTO(List<RangoItemRequestDTO> rangos) {
        this.rangos = rangos;
    }

    public List<RangoItemRequestDTO> getRangos() {
        return rangos;
    }

    public void setRangos(List<RangoItemRequestDTO> rangos) {
        this.rangos = rangos;
    }
}
