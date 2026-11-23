package com.udea.ScoreCrediticio.Exceptions;

/**
 * El rango de fechas indicado para consultar el dashboard o exportar reportes
 * es inconsistente (por ejemplo, fechaInicio posterior a fechaFin).
 */
public class RangoFechasInvalidoException extends RuntimeException {

    public RangoFechasInvalidoException(String message) {
        super(message);
    }
}
