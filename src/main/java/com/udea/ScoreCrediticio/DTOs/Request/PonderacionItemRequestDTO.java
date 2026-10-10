package com.udea.ScoreCrediticio.DTOs.Request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public class PonderacionItemRequestDTO {

    @NotNull(message = "El id de la variable es obligatorio")
    private Long variableId;

    @NotNull(message = "El peso es obligatorio")
    @DecimalMin(value = "0.0001", message = "El peso debe ser mayor que 0")
    @DecimalMax(value = "100.0000", message = "El peso no puede superar el 100%")
    @Digits(integer = 3, fraction = 4, message = "El peso tiene un formato inválido")
    private BigDecimal peso;

    public PonderacionItemRequestDTO() {
    }

    public PonderacionItemRequestDTO(Long variableId, BigDecimal peso) {
        this.variableId = variableId;
        this.peso = peso;
    }

    public Long getVariableId() {
        return variableId;
    }

    public void setVariableId(Long variableId) {
        this.variableId = variableId;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }
}
