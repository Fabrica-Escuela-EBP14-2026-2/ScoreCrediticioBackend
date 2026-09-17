package com.udea.ScoreCrediticio.DTOs.Request;

import java.math.BigDecimal;

import com.udea.ScoreCrediticio.Model.TipoDocumento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PerfilFinancieroRequestDTO {

    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento no debe superar los 20 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El número de documento tiene un formato inválido")
    private String numeroDocumento;

    @NotNull(message = "Los ingresos mensuales son obligatorios")
    @DecimalMin(value = "0.00", inclusive = true, message = "Los ingresos deben ser mayor o igual a 0")
    @Digits(integer = 15, fraction = 2, message = "Los ingresos tienen un formato inválido")
    private BigDecimal ingresos;

    @NotNull(message = "Los egresos fijos son obligatorios")
    @DecimalMin(value = "0.00", inclusive = true, message = "Los egresos deben ser mayor o igual a 0")
    @Digits(integer = 15, fraction = 2, message = "Los egresos tienen un formato inválido")
    private BigDecimal egresos;

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

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
