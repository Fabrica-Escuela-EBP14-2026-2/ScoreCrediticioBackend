package com.udea.ScoreCrediticio.Model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Snapshot de una variable de riesgo usada en una evaluacion (HU12).
 * Guarda el nombre, el valor y el peso tal como estaban al momento del calculo,
 * de modo que cambios posteriores en el catalogo o en la formula no alteren
 * el historial.
 */
@Entity
@Table(name = "variable_evaluada")
public class VariableEvaluada {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historial_id", nullable = false)
    private HistorialEvaluacion historial;

    @Column(name = "variable_id", nullable = false)
    private Long variableId;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "valor", nullable = false, precision = 7, scale = 4)
    private BigDecimal valor;

    @Column(name = "peso", nullable = false, precision = 7, scale = 4)
    private BigDecimal peso;

    public VariableEvaluada() {
    }

    public VariableEvaluada(Long variableId, String nombre, BigDecimal valor, BigDecimal peso) {
        this.variableId = variableId;
        this.nombre = nombre;
        this.valor = valor;
        this.peso = peso;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public HistorialEvaluacion getHistorial() {
        return historial;
    }

    public void setHistorial(HistorialEvaluacion historial) {
        this.historial = historial;
    }

    public Long getVariableId() {
        return variableId;
    }

    public void setVariableId(Long variableId) {
        this.variableId = variableId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }
}
