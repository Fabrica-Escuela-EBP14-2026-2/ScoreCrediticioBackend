package com.udea.ScoreCrediticio.DTOs.Response;

import java.time.LocalDateTime;

import com.udea.ScoreCrediticio.Model.NivelRiesgo;

/**
 * Evaluacion del historial de un solicitante (HU12): resultado, fecha/hora,
 * usuario evaluador y snapshot de los datos vigentes al momento del calculo.
 */
public class HistorialEvaluacionResponseDTO {

    private Long id;
    private Long solicitanteId;
    private Integer puntaje;
    private NivelRiesgo nivel;
    private LocalDateTime fechaCalculo;
    private String evaluadorEmail;
    private DetalleEvaluacionResponseDTO detalle;

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

    public Integer getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }

    public void setNivel(NivelRiesgo nivel) {
        this.nivel = nivel;
    }

    public LocalDateTime getFechaCalculo() {
        return fechaCalculo;
    }

    public void setFechaCalculo(LocalDateTime fechaCalculo) {
        this.fechaCalculo = fechaCalculo;
    }

    public String getEvaluadorEmail() {
        return evaluadorEmail;
    }

    public void setEvaluadorEmail(String evaluadorEmail) {
        this.evaluadorEmail = evaluadorEmail;
    }

    public DetalleEvaluacionResponseDTO getDetalle() {
        return detalle;
    }

    public void setDetalle(DetalleEvaluacionResponseDTO detalle) {
        this.detalle = detalle;
    }
}
