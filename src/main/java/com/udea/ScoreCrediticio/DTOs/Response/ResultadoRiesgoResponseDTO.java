package com.udea.ScoreCrediticio.DTOs.Response;

import java.time.LocalDateTime;

import com.udea.ScoreCrediticio.Model.NivelRiesgo;

/**
 * Resultado del calculo de riesgo de un solicitante (HU10): puntaje 0-1000,
 * nivel clasificado con la escala vigente y fecha/hora del calculo.
 */
public class ResultadoRiesgoResponseDTO {

    private Long solicitanteId;
    private Integer puntaje;
    private NivelRiesgo nivel;
    private LocalDateTime fechaCalculo;
    private String mensaje;

    public ResultadoRiesgoResponseDTO() {
    }

    public ResultadoRiesgoResponseDTO(Long solicitanteId, Integer puntaje, NivelRiesgo nivel,
            LocalDateTime fechaCalculo, String mensaje) {
        this.solicitanteId = solicitanteId;
        this.puntaje = puntaje;
        this.nivel = nivel;
        this.fechaCalculo = fechaCalculo;
        this.mensaje = mensaje;
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

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
