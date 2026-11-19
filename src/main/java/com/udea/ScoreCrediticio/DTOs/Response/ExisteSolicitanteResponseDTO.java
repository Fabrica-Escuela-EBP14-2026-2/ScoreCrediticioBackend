package com.udea.ScoreCrediticio.DTOs.Response;

/**
 * Resultado de la verificacion previa de documento usada por el formulario
 * para resaltar el campo antes de enviar el registro.
 */
public class ExisteSolicitanteResponseDTO {

    private boolean existe;

    public ExisteSolicitanteResponseDTO() {
    }

    public ExisteSolicitanteResponseDTO(boolean existe) {
        this.existe = existe;
    }

    public boolean isExiste() {
        return existe;
    }

    public void setExiste(boolean existe) {
        this.existe = existe;
    }
}
