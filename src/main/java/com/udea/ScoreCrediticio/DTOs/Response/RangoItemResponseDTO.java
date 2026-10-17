package com.udea.ScoreCrediticio.DTOs.Response;

import com.udea.ScoreCrediticio.Model.NivelRiesgo;

public class RangoItemResponseDTO {

    private NivelRiesgo nivel;
    private Integer min;
    private Integer max;

    public RangoItemResponseDTO() {
    }

    public RangoItemResponseDTO(NivelRiesgo nivel, Integer min, Integer max) {
        this.nivel = nivel;
        this.min = min;
        this.max = max;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }

    public void setNivel(NivelRiesgo nivel) {
        this.nivel = nivel;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }
}
