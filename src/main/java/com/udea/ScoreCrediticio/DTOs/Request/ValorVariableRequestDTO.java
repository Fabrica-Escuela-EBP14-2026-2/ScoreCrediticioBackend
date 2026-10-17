package com.udea.ScoreCrediticio.DTOs.Request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Valor de una variable de riesgo dentro del calculo de riesgo (HU10).
 * La ficha del solicitante asigna a cada variable un valor numerico entre 0 y 100.
 */
public class ValorVariableRequestDTO {

    @NotNull(message = "El id de la variable es obligatorio")
    private Long variableId;

    @NotNull(message = "El valor de la variable es obligatorio")
    @DecimalMin(value = "0", message = "El valor de la variable no puede ser menor que 0")
    @DecimalMax(value = "100", message = "El valor de la variable no puede superar 100")
    private BigDecimal valor;

    public ValorVariableRequestDTO() {
    }

    public ValorVariableRequestDTO(Long variableId, BigDecimal valor) {
        this.variableId = variableId;
        this.valor = valor;
    }

    public Long getVariableId() {
        return variableId;
    }

    public void setVariableId(Long variableId) {
        this.variableId = variableId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
