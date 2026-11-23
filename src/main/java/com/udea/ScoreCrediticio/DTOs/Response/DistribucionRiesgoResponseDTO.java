package com.udea.ScoreCrediticio.DTOs.Response;

import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta agregada del dashboard de distribucion de riesgo de cartera (HU13).
 * Incluye siempre las tres categorias (Bajo, Medio, Alto), incluso con valores en cero.
 */
public class DistribucionRiesgoResponseDTO {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private long totalSolicitantes;
    private List<DistribucionCategoriaRiesgoDTO> distribucion;
    private String mensaje;

    public DistribucionRiesgoResponseDTO() {
    }

    public DistribucionRiesgoResponseDTO(LocalDate fechaInicio, LocalDate fechaFin,
            long totalSolicitantes, List<DistribucionCategoriaRiesgoDTO> distribucion, String mensaje) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.totalSolicitantes = totalSolicitantes;
        this.distribucion = distribucion;
        this.mensaje = mensaje;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public long getTotalSolicitantes() {
        return totalSolicitantes;
    }

    public void setTotalSolicitantes(long totalSolicitantes) {
        this.totalSolicitantes = totalSolicitantes;
    }

    public List<DistribucionCategoriaRiesgoDTO> getDistribucion() {
        return distribucion;
    }

    public void setDistribucion(List<DistribucionCategoriaRiesgoDTO> distribucion) {
        this.distribucion = distribucion;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
