package com.udea.ScoreCrediticio.DTOs.Response;

import java.time.LocalDateTime;

import com.udea.ScoreCrediticio.Model.NivelRiesgo;
import com.udea.ScoreCrediticio.Model.TipoDocumento;

/**
 * Proyeccion plana de un solicitante y una evaluacion de riesgo para el reporte
 * descargable de cartera (HU14).
 */
public class CarteraReporteFilaDTO {

    private final TipoDocumento tipoDocumento;
    private final String numeroDocumento;
    private final String nombre;
    private final String apellido;
    private final LocalDateTime fechaEvaluacion;
    private final Integer puntaje;
    private final NivelRiesgo nivel;

    public CarteraReporteFilaDTO(TipoDocumento tipoDocumento, String numeroDocumento,
            String nombre, String apellido, LocalDateTime fechaEvaluacion,
            Integer puntaje, NivelRiesgo nivel) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaEvaluacion = fechaEvaluacion;
        this.puntaje = puntaje;
        this.nivel = nivel;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public LocalDateTime getFechaEvaluacion() {
        return fechaEvaluacion;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }
}
