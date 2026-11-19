package com.udea.ScoreCrediticio.DTOs.Response;

import java.math.BigDecimal;

/**
 * Variable de riesgo con el valor y el peso que tenia al momento del calculo (HU12).
 */
public class VariableEvaluadaResponseDTO {

    private Long variableId;
    private String nombre;
    private BigDecimal valor;
    private BigDecimal peso;

    public Long getVariableId() {
        return variableId;
    }

    public void setVariableId(Long variableId) {
        this.variableId = variableId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }
}
