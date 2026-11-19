package com.udea.ScoreCrediticio.Exceptions;

import java.util.List;

/**
 * El solicitante no tiene todos sus datos obligatorios o la peticion no cubre
 * todas las variables activas de la formula vigente (HU11).
 */
public class DatosIncompletosException extends RuntimeException {

    private final List<String> camposFaltantes;
    private final List<String> variablesFaltantes;

    public DatosIncompletosException(String message, List<String> camposFaltantes,
            List<String> variablesFaltantes) {
        super(message);
        this.camposFaltantes = List.copyOf(camposFaltantes);
        this.variablesFaltantes = List.copyOf(variablesFaltantes);
    }

    public List<String> getCamposFaltantes() {
        return camposFaltantes;
    }

    public List<String> getVariablesFaltantes() {
        return variablesFaltantes;
    }
}
