package com.udea.ScoreCrediticio.DTOs.Response;

import java.time.OffsetDateTime;

import tools.jackson.databind.JsonNode;
import com.udea.ScoreCrediticio.Model.AccionAuditoria;
import com.udea.ScoreCrediticio.Model.ModuloAuditoria;

public class AuditoriaResponseDTO {

    private Long id;
    private String usuario;
    private OffsetDateTime fechaUtc;
    private ModuloAuditoria modulo;
    private AccionAuditoria accion;
    private JsonNode valorAnterior;
    private JsonNode valorNuevo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public OffsetDateTime getFechaUtc() {
        return fechaUtc;
    }

    public void setFechaUtc(OffsetDateTime fechaUtc) {
        this.fechaUtc = fechaUtc;
    }

    public ModuloAuditoria getModulo() {
        return modulo;
    }

    public void setModulo(ModuloAuditoria modulo) {
        this.modulo = modulo;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public void setAccion(AccionAuditoria accion) {
        this.accion = accion;
    }

    public JsonNode getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(JsonNode valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public JsonNode getValorNuevo() {
        return valorNuevo;
    }

    public void setValorNuevo(JsonNode valorNuevo) {
        this.valorNuevo = valorNuevo;
    }
}
