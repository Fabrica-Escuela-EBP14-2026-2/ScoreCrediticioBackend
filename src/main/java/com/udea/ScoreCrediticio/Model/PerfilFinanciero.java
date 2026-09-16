package com.udea.ScoreCrediticio.Model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;


@Entity 
public class PerfilFinanciero {
    
    @Id 
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "ingresos", nullable = false, precision = 19, scale = 2)
    private BigDecimal ingresos;

    @Column(name = "egresos", nullable = false, precision = 19, scale = 2)
    private BigDecimal egresos;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", unique = true, nullable = false)
    private Solicitante solicitante;

    public PerfilFinanciero() {
    }

    public PerfilFinanciero(Long id, BigDecimal ingresos, BigDecimal egresos, Solicitante solicitante) {
        this.id = id;
        this.ingresos = ingresos;
        this.egresos = egresos;
        this.solicitante = solicitante;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Solicitante getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Solicitante solicitante) {
        this.solicitante = solicitante;
    }

    @Transient
    public BigDecimal getIngresoNetoDisponible() {
        if (ingresos == null || egresos == null) {
            return null;
        }
        return ingresos.subtract(egresos);
    }

    public String toString() {
        return "PerfilFinanciero [id=" + id + ", ingresos=" + ingresos + ", egresos=" + egresos
                + ", ingresoNetoDisponible=" + getIngresoNetoDisponible() + "]";
    }
}
