package com.udea.ScoreCrediticio.Model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Nivel de riesgo crediticio que el Administrador asocia a un rango de puntaje (HU09).
 * Se serializa como "Bajo"/"Medio"/"Alto" para coincidir con la interfaz, pero acepta
 * tambien la forma en mayusculas ("BAJO") al deserializar.
 */
public enum NivelRiesgo {

    BAJO("Bajo"),
    MEDIO("Medio"),
    ALTO("Alto");

    private final String etiqueta;

    NivelRiesgo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    @JsonCreator
    public static NivelRiesgo fromValor(String valor) {
        if (valor == null) {
            return null;
        }

        String normalizado = valor.trim().toUpperCase();
        for (NivelRiesgo nivel : values()) {
            if (nivel.name().equals(normalizado)) {
                return nivel;
            }
        }

        throw new IllegalArgumentException("Nivel de riesgo no valido: " + valor);
    }
}
