package com.udea.ScoreCrediticio.Exceptions;

public class SumaPesosInvalidaException extends RuntimeException {

    private final String totalCalculado;
    private final String diferencia;
    private final String tipoDescuadre; // "FALTANTE" o "EXCEDENTE"

    public SumaPesosInvalidaException(String message, String totalCalculado, String diferencia, String tipoDescuadre) {
        super(message);
        this.totalCalculado = totalCalculado;
        this.diferencia = diferencia;
        this.tipoDescuadre = tipoDescuadre;
    }

    public String getTotalCalculado() {
        return totalCalculado;
    }

    public String getDiferencia() {
        return diferencia;
    }

    public String getTipoDescuadre() {
        return tipoDescuadre;
    }
}
