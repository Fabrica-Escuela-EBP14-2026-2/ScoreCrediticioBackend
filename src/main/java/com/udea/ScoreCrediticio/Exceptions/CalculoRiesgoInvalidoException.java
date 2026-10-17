package com.udea.ScoreCrediticio.Exceptions;

/**
 * Los valores enviados para el calculo de riesgo no cubren exactamente las
 * variables de la formula vigente: hay variables sin valor, variables ajenas
 * a la formula o ids repetidos (HU10).
 */
public class CalculoRiesgoInvalidoException extends RuntimeException {

    public CalculoRiesgoInvalidoException(String message) {
        super(message);
    }
}
