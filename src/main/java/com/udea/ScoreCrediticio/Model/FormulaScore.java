package com.udea.ScoreCrediticio.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Entidad que representa la formula global de scoring ponderada por el Administrador de Riesgos.
 * Solo puede haber una formula vigente a la vez, garantizado a nivel de base de datos
 * por la restriccion UNIQUE en clave_vigencia (donde null es permitido multiples veces
 * para formulas historicas, pero el valor "VIGENTE" es unico).
 */
@Entity
@Table(name = "formula_score")
public class FormulaScore {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "vigente", nullable = false)
    private boolean vigente;

    /**
     * Columna con restriccion UNIQUE en BD para garantizar que solo una fila tenga el valor "VIGENTE".
     * Cuando una formula pasa a ser historica/no vigente, este campo se establece en null.
     */
    @Column(name = "clave_vigencia", unique = true, length = 20)
    private String claveVigencia;

    @Column(name = "suma_total", nullable = false, precision = 7, scale = 4)
    private BigDecimal sumaTotal;

    @CreationTimestamp
    @Column(name = "fecha_implementacion", nullable = false, updatable = false)
    private LocalDateTime fechaImplementacion;

    @OneToMany(mappedBy = "formula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PonderacionVariable> ponderaciones = new ArrayList<>();

    public FormulaScore() {
    }

    public FormulaScore(String nombre, boolean vigente, String claveVigencia, BigDecimal sumaTotal) {
        this.nombre = nombre;
        this.vigente = vigente;
        this.claveVigencia = claveVigencia;
        this.sumaTotal = sumaTotal;
    }

    public void agregarPonderacion(PonderacionVariable ponderacion) {
        ponderaciones.add(ponderacion);
        ponderacion.setFormula(this);
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

    public boolean isVigente() {
        return vigente;
    }

    public void setVigente(boolean vigente) {
        this.vigente = vigente;
        this.claveVigencia = vigente ? "VIGENTE" : null;
    }

    public String getClaveVigencia() {
        return claveVigencia;
    }

    public void setClaveVigencia(String claveVigencia) {
        this.claveVigencia = claveVigencia;
    }

    public BigDecimal getSumaTotal() {
        return sumaTotal;
    }

    public void setSumaTotal(BigDecimal sumaTotal) {
        this.sumaTotal = sumaTotal;
    }

    public LocalDateTime getFechaImplementacion() {
        return fechaImplementacion;
    }

    public void setFechaImplementacion(LocalDateTime fechaImplementacion) {
        this.fechaImplementacion = fechaImplementacion;
    }

    public List<PonderacionVariable> getPonderaciones() {
        return ponderaciones;
    }

    public void setPonderaciones(List<PonderacionVariable> ponderaciones) {
        this.ponderaciones = ponderaciones;
    }
}
