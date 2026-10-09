package com.udea.ScoreCrediticio.DTOs.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class FormulaResponseDTO {

    private Long id;
    private String mensaje;
    private String estado;
    private boolean vigente;
    private BigDecimal sumaTotal;
    private LocalDateTime fechaImplementacion;
    private List<PonderacionItemResponseDTO> ponderaciones;

    public FormulaResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isVigente() {
        return vigente;
    }

    public void setVigente(boolean vigente) {
        this.vigente = vigente;
    }

    public BigDecimal getSumaTotal() {
        return sumaTotal;
    }

    public void setSumaTotal(BigDecimal sumaTotal) {
        this.sumaTotal = sumaTotal;
    }

    public LocalDateTime getFechaImplementacion() {
        return fechaImplementacion;
    }

    public void setFechaImplementacion(LocalDateTime fechaImplementacion) {
        this.fechaImplementacion = fechaImplementacion;
    }

    public List<PonderacionItemResponseDTO> getPonderaciones() {
        return ponderaciones;
    }

    public void setPonderaciones(List<PonderacionItemResponseDTO> ponderaciones) {
        this.ponderaciones = ponderaciones;
    }
}
