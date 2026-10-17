package com.udea.ScoreCrediticio.Exceptions;

/**
 * Configuracion de rangos invalida: niveles duplicados o faltantes, o un limite
 * minimo mayor que el maximo dentro de un mismo nivel (HU09).
 */
public class RangoInvalidoException extends RuntimeException {

    public RangoInvalidoException(String message) {
        super(message);
    }
}
