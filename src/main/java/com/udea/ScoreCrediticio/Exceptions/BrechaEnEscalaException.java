package com.udea.ScoreCrediticio.Exceptions;

import java.util.List;

/**
 * Se lanza cuando la escala deja valores de puntaje sin clasificar (HU09, escenario 3).
 * Lleva los intervalos descubiertos (por ejemplo "301-399" o "800-1000") que no
 * quedaron cubiertos por ningun nivel de riesgo.
 */
public class BrechaEnEscalaException extends RuntimeException {

    private final List<String> intervalosSinCobertura;

    public BrechaEnEscalaException(String message, List<String> intervalosSinCobertura) {
        super(message);
        this.intervalosSinCobertura = intervalosSinCobertura;
    }

    public List<String> getIntervalosSinCobertura() {
        return intervalosSinCobertura;
    }
}
