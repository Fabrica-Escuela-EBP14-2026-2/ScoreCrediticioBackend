package com.udea.ScoreCrediticio.Exceptions;

/**
 * El formato solicitado para exportar el reporte de cartera no esta soportado.
 */
public class FormatoReporteInvalidoException extends RuntimeException {

    public FormatoReporteInvalidoException(String message) {
        super(message);
    }
}
