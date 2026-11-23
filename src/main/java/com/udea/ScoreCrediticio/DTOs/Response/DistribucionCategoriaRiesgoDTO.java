package com.udea.ScoreCrediticio.DTOs.Response;

import java.math.BigDecimal;

import com.udea.ScoreCrediticio.Model.NivelRiesgo;

/**
 * Total y porcentaje de solicitantes clasificados en una categoria de riesgo (HU13).
 */
public class DistribucionCategoriaRiesgoDTO {

    private NivelRiesgo nivel;
    private long total;
    private BigDecimal porcentaje;

    public DistribucionCategoriaRiesgoDTO() {
    }

    public DistribucionCategoriaRiesgoDTO(NivelRiesgo nivel, long total, BigDecimal porcentaje) {
        this.nivel = nivel;
        this.total = total;
        this.porcentaje = porcentaje;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }

    public void setNivel(NivelRiesgo nivel) {
        this.nivel = nivel;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }
}
