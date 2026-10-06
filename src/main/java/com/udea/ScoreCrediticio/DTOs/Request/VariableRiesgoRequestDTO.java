package com.udea.ScoreCrediticio.DTOs.Request;

import java.math.BigDecimal;

import com.udea.ScoreCrediticio.Model.TipoDatoVariable;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VariableRiesgoRequestDTO {

    @NotBlank(message = "El nombre de la variable es obligatorio")
    @Size(max = 100, message = "El nombre de la variable no debe superar los 100 caracteres")
    private String nombre;

    // Valores admitidos: NUMERICO, PORCENTAJE, CATEGORICO
    @NotNull(message = "El tipo de dato es obligatorio")
    private TipoDatoVariable tipoDato;

    @NotNull(message = "El peso es obligatorio")
    @DecimalMin(value = "0.0001", message = "El peso debe ser mayor que 0")
    @DecimalMax(value = "100.0000", message = "El peso no puede superar el 100%")
    @Digits(integer = 3, fraction = 4, message = "El peso tiene un formato inválido")
    private BigDecimal peso;

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
}
