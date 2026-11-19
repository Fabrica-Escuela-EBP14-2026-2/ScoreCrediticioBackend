package com.udea.ScoreCrediticio.DTOs.Response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Datos financieros y variables que tenia el solicitante cuando se ejecuto
 * el calculo (HU12).
 */
public class DetalleEvaluacionResponseDTO {

    private BigDecimal ingresos;
    private BigDecimal egresos;
    private BigDecimal ingresoNetoDisponible;
    private List<VariableEvaluadaResponseDTO> variables = new ArrayList<>();

    public BigDecimal getIngresos() {
        return ingresos;
    }

    public void setIngresos(BigDecimal ingresos) {
        this.ingresos = ingresos;
    }

    public BigDecimal getEgresos() {
        return egresos;
    }

    public void setEgresos(BigDecimal egresos) {
        this.egresos = egresos;
    }

    public BigDecimal getIngresoNetoDisponible() {
        return ingresoNetoDisponible;
    }

    public void setIngresoNetoDisponible(BigDecimal ingresoNetoDisponible) {
        this.ingresoNetoDisponible = ingresoNetoDisponible;
    }

    public List<VariableEvaluadaResponseDTO> getVariables() {
        return variables;
    }

    public void setVariables(List<VariableEvaluadaResponseDTO> variables) {
        this.variables = variables;
    }
}
