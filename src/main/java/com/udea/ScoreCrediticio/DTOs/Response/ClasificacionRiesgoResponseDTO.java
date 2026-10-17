package com.udea.ScoreCrediticio.DTOs.Response;

import com.udea.ScoreCrediticio.Model.NivelRiesgo;

/**
 * Resultado de clasificar un puntaje dentro de la escala de riesgo vigente.
 */
public class ClasificacionRiesgoResponseDTO {

    private Integer puntaje;
    private NivelRiesgo nivel;
    private String mensaje;

    public ClasificacionRiesgoResponseDTO() {
    }

    public ClasificacionRiesgoResponseDTO(Integer puntaje, NivelRiesgo nivel, String mensaje) {
        this.puntaje = puntaje;
        this.nivel = nivel;
        this.mensaje = mensaje;
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

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
