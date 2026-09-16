package com.udea.ScoreCrediticio.DTOs.Request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public class PerfilFinancieroRequestDTO {

    @NotNull(message = "Los ingresos mensuales son obligatorios")
    @DecimalMin(value = "0.00", inclusive = true, message = "Los ingresos deben ser mayor o igual a 0")
    @Digits(integer = 15, fraction = 2, message = "Los ingresos tienen un formato inválido")
    private BigDecimal ingresos;

    @NotNull(message = "Los egresos fijos son obligatorios")
    @DecimalMin(value = "0.00", inclusive = true, message = "Los egresos deben ser mayor o igual a 0")
    @Digits(integer = 15, fraction = 2, message = "Los egresos tienen un formato inválido")
    private BigDecimal egresos;

    public BigDecimal getIngresos() {
        return ingresos;
    }

    public void setIngresos(BigDecimal ingresos) {
        this.ingresos = ingresos;
    }

    public BigDecimal getEgresos() {
        return egresos;
    }

    public void setEgresos(BigDecimal egresos) {
        this.egresos = egresos;
    }
}
