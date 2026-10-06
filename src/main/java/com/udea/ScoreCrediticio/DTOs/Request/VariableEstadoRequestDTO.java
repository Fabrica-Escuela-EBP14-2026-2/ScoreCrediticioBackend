package com.udea.ScoreCrediticio.DTOs.Request;

import com.udea.ScoreCrediticio.Model.EstadoVariable;

import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de PATCH /api/variables/{id}/estado. Solo se cambia el estado: el nombre,
 * el tipo de dato, el peso y la fecha de registro se conservan tal como quedaron,
 * para no perder el historial de configuracion del catalogo.
 * Admite "Activa"/"Inactiva" o "ACTIVA"/"INACTIVA".
 */
public class VariableEstadoRequestDTO {

    @NotNull(message = "El estado es obligatorio")
    private EstadoVariable estado;

    public EstadoVariable getEstado() {
        return estado;
    }

    public void setEstado(EstadoVariable estado) {
        this.estado = estado;
    }
}