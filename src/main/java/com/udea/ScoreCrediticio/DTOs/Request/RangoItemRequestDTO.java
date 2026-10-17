package com.udea.ScoreCrediticio.DTOs.Request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Un rango de la escala de riesgo: nivel (Bajo/Medio/Alto) y sus limites inclusivos
 * de puntaje minimo y maximo dentro de la escala 0-1000.
 */
public class RangoItemRequestDTO {

    // Acepta tanto "nivel" como "level" para tolerar el nombre usado por el cliente.
    @JsonAlias("level")
    @NotNull(message = "El nivel de riesgo es obligatorio")
    private NivelRiesgo nivel;

    @NotNull(message = "El puntaje minimo es obligatorio")
    @Min(value = 0, message = "El puntaje minimo no puede ser menor que 0")
    @Max(value = 1000, message = "El puntaje minimo no puede superar 1000")
    private Integer min;

    @NotNull(message = "El puntaje maximo es obligatorio")
    @Min(value = 0, message = "El puntaje maximo no puede ser menor que 0")
    @Max(value = 1000, message = "El puntaje maximo no puede superar 1000")
    private Integer max;

    public RangoItemRequestDTO() {
    }

    public RangoItemRequestDTO(NivelRiesgo nivel, Integer min, Integer max) {
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
