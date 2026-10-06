package com.udea.ScoreCrediticio.Exceptions;

public class PesoTotalExcedeLimiteException extends RuntimeException {

    private final String pesoTotalActual;
    private final String pesoSolicitado;
    private final String pesoTotalResultante;

    public PesoTotalExcedeLimiteException(String message, String pesoTotalActual,
            String pesoSolicitado, String pesoTotalResultante) {
        super(message);
        this.pesoTotalActual = pesoTotalActual;
        this.pesoSolicitado = pesoSolicitado;
        this.pesoTotalResultante = pesoTotalResultante;
    }

    public String getPesoTotalActual() {
        return pesoTotalActual;
    }

    public String getPesoSolicitado() {
        return pesoSolicitado;
    }

    public String getPesoTotalResultante() {
        return pesoTotalResultante;
    }
}
