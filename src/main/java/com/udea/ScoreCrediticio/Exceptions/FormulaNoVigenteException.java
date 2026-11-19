package com.udea.ScoreCrediticio.Exceptions;

/**
 * No existe una formula de scoring vigente y completa para ejecutar la
 * evaluacion de riesgo del solicitante (HU11).
 */
public class FormulaNoVigenteException extends RuntimeException {

    public FormulaNoVigenteException(String message) {
        super(message);
    }
}
