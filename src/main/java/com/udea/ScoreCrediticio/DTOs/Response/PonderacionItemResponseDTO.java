package com.udea.ScoreCrediticio.DTOs.Response;

import java.math.BigDecimal;

import com.udea.ScoreCrediticio.Model.TipoDatoVariable;

public class PonderacionItemResponseDTO {

    private Long variableId;
    private String nombreVariable;
    private TipoDatoVariable tipoDato;
    private BigDecimal peso;

    public PonderacionItemResponseDTO() {
    }

    public PonderacionItemResponseDTO(Long variableId, String nombreVariable, TipoDatoVariable tipoDato, BigDecimal peso) {
        this.variableId = variableId;
        this.nombreVariable = nombreVariable;
        this.tipoDato = tipoDato;
        this.peso = peso;
    }

    public Long getVariableId() {
        return variableId;
    }

    public void setVariableId(Long variableId) {
        this.variableId = variableId;
    }

    public String getNombreVariable() {
        return nombreVariable;
    }

    public void setNombreVariable(String nombreVariable) {
        this.nombreVariable = nombreVariable;
    }

    public TipoDatoVariable getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(TipoDatoVariable tipoDato) {
        this.tipoDato = tipoDato;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }
}
