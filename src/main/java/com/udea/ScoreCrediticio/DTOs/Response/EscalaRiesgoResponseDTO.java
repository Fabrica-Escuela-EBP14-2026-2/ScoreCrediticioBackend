package com.udea.ScoreCrediticio.DTOs.Response;

import java.util.List;

public class EscalaRiesgoResponseDTO {

    private String mensaje;
    private List<RangoItemResponseDTO> rangos;

    public EscalaRiesgoResponseDTO() {
    }

    public EscalaRiesgoResponseDTO(String mensaje, List<RangoItemResponseDTO> rangos) {
        this.mensaje = mensaje;
        this.rangos = rangos;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public List<RangoItemResponseDTO> getRangos() {
        return rangos;
    }

    public void setRangos(List<RangoItemResponseDTO> rangos) {
        this.rangos = rangos;
    }
}
