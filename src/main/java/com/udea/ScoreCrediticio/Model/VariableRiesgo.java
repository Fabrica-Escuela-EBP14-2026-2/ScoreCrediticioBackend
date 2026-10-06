package com.udea.ScoreCrediticio.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Variable de riesgo parametrizable por el Administrador de Riesgos.
 * El nombre es unico: no puede haber dos variables con el mismo nombre
 * (comparacion sin distincion de mayusculas y minusculas en el servicio,
 * y unicidad exacta garantizada por la base de datos).
 */
@Entity
@Table(name = "variable_riesgo")
public class VariableRiesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dato", nullable = false, length = 20)
    private TipoDatoVariable tipoDato;

    @Column(name = "peso", nullable = false, precision = 7, scale = 4)
    private BigDecimal peso;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoVariable estado;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public VariableRiesgo() {
    }

    public VariableRiesgo(String nombre, TipoDatoVariable tipoDato, BigDecimal peso, EstadoVariable estado) {
        this.nombre = nombre;
        this.tipoDato = tipoDato;
        this.peso = peso;
        this.estado = estado;
    }

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

    public String toString() {
        return "VariableRiesgo [id=" + id + ", nombre=" + nombre + ", tipoDato=" + tipoDato + ", peso=" + peso
                + ", estado=" + estado + ", fechaRegistro=" + fechaRegistro + "]";
    }
}
