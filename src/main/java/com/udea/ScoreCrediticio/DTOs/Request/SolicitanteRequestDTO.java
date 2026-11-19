package com.udea.ScoreCrediticio.DTOs.Request;


import com.udea.ScoreCrediticio.Model.TipoDocumento;
import com.udea.ScoreCrediticio.Security.NombreSanitizadoDeserializer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;

public class SolicitanteRequestDTO {
    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento no debe superar los 20 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El número de documento tiene un formato inválido")
    private String numeroDocumento;

    @JsonDeserialize(using = NombreSanitizadoDeserializer.class)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no debe superar los 100 caracteres")
    private String nombre;

    @JsonDeserialize(using = NombreSanitizadoDeserializer.class)
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no debe superar los 100 caracteres")
    private String apellido;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^(?:[0-9]{7}|[0-9]{10})$",
            message = "El teléfono debe tener exactamente 7 o 10 dígitos")
    private String telefono;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 255, message = "El correo no debe superar los 255 caracteres")
    private String email;

    // Getters y setters
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
