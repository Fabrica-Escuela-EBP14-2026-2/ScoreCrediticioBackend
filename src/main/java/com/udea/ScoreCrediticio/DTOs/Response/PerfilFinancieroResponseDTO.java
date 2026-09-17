package com.udea.ScoreCrediticio.DTOs.Response;

import java.math.BigDecimal;

import com.udea.ScoreCrediticio.Model.TipoDocumento;

public class PerfilFinancieroResponseDTO {
    private Long id;
    private Long solicitanteId;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private BigDecimal ingresos;
    private BigDecimal egresos;
    private BigDecimal ingresoNetoDisponible;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSolicitanteId() {
        return solicitanteId;
    }

    public void setSolicitanteId(Long solicitanteId) {
        this.solicitanteId = solicitanteId;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

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
}
