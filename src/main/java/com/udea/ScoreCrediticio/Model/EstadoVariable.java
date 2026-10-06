package com.udea.ScoreCrediticio.Model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Estado de una variable de riesgo dentro del catálogo de evaluación.
 * Se serializa como "Activa"/"Inactiva" para coincidir con la interfaz, pero acepta
 * cualquiera de las dos formas ("ACTIVA" o "Activa") al deserializar.
 */
public enum EstadoVariable {

    ACTIVA("Activa"),
    INACTIVA("Inactiva");

    private final String etiqueta;

    EstadoVariable(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    @JsonCreator
    public static EstadoVariable fromValor(String valor) {
        if (valor == null) {
            return null;
        }

        String normalizado = valor.trim().toUpperCase();
        for (EstadoVariable estado : values()) {
            if (estado.name().equals(normalizado)) {
                return estado;
            }
        }

        throw new IllegalArgumentException("Estado de variable no válido: " + valor);
    }
}
