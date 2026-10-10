package com.udea.ScoreCrediticio.DTOs.Request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public class DefinirFormulaRequestDTO {

    @NotEmpty(message = "La lista de ponderaciones no puede estar vacía")
    @Valid
    private List<PonderacionItemRequestDTO> ponderaciones;

    public DefinirFormulaRequestDTO() {
    }

    public DefinirFormulaRequestDTO(List<PonderacionItemRequestDTO> ponderaciones) {
        this.ponderaciones = ponderaciones;
    }

    public List<PonderacionItemRequestDTO> getPonderaciones() {
        return ponderaciones;
    }

    public void setPonderaciones(List<PonderacionItemRequestDTO> ponderaciones) {
        this.ponderaciones = ponderaciones;
    }
}
