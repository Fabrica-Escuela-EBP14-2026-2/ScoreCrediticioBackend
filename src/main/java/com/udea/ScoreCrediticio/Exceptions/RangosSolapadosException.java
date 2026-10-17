package com.udea.ScoreCrediticio.Exceptions;

import java.util.List;

/**
 * Se lanza cuando dos o mas rangos de la escala se cruzan entre si (HU09, escenario 2).
 * Lleva los campos en conflicto (por ejemplo "Medio-min", "Alto-max") para que el
 * cliente pueda resaltar los limites que se solapan.
 */
public class RangosSolapadosException extends RuntimeException {

    private final List<String> camposConflicto;
    private final List<String> nivelesConflicto;

    public RangosSolapadosException(String message, List<String> camposConflicto, List<String> nivelesConflicto) {
        super(message);
        this.camposConflicto = camposConflicto;
        this.nivelesConflicto = nivelesConflicto;
    }

    public List<String> getCamposConflicto() {
        return camposConflicto;
    }

    public List<String> getNivelesConflicto() {
        return nivelesConflicto;
    }
}
