package com.udea.ScoreCrediticio.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class Solicitante {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "email")
    private String email;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento")
    private TipoDocumento tipoDocumento;

    @OneToOne(mappedBy = "solicitante", fetch = FetchType.LAZY)
    private PerfilFinanciero perfilFinanciero;

    public Solicitante() {
    }

    public Solicitante(Long id, String nombre, String apellido, String telefono, String email, String numeroDocumento,
            TipoDocumento tipoDocumento) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.email = email;
        this.numeroDocumento = numeroDocumento;
        this.tipoDocumento = tipoDocumento;
    }

    public Long getId() {
        return id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public PerfilFinanciero getPerfilFinanciero() {
        return perfilFinanciero;
    }

    public void setPerfilFinanciero(PerfilFinanciero perfilFinanciero) {
        this.perfilFinanciero = perfilFinanciero;
    }

    public String toString() {
        return "Solicitante [id=" + id + ", nombre=" + nombre + ", apellido=" + apellido + ", telefono=" + telefono
                + ", email=" + email + ", numeroDocumento=" + numeroDocumento + ", tipoDocumento=" + tipoDocumento
                + "]";
    }
}
