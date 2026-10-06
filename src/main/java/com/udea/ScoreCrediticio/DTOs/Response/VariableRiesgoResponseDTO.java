package com.udea.ScoreCrediticio.DTOs.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.udea.ScoreCrediticio.Model.EstadoVariable;
import com.udea.ScoreCrediticio.Model.TipoDatoVariable;

public class VariableRiesgoResponseDTO {

    private Long id;
    private String nombre;
    private TipoDatoVariable tipoDato;
    private BigDecimal peso;
    private EstadoVariable estado;
    private LocalDateTime fechaRegistro;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoDatoVariable getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(TipoDatoVariable tipoDato) {
        this.tipoDato = tipoDato;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }

    public EstadoVariable getEstado() {
        return estado;
    }

    public void setEstado(EstadoVariable estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
