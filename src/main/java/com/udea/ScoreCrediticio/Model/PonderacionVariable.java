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
 * Ponderacion individual asignada a una variable de riesgo activa dentro de una formula global.
 * Se almacena de forma independiente del peso inicial de la variable (HU06) para no sobreescribir
 * datos de otros modulos.
 */
@Entity
@Table(name = "ponderacion_variable")
public class PonderacionVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "formula_id", nullable = false)
    private FormulaScore formula;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "variable_id", nullable = false)
    private VariableRiesgo variable;

    @Column(name = "peso", nullable = false, precision = 7, scale = 4)
    private BigDecimal peso;

    public PonderacionVariable() {
    }

    public PonderacionVariable(FormulaScore formula, VariableRiesgo variable, BigDecimal peso) {
        this.formula = formula;
        this.variable = variable;
        this.peso = peso;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FormulaScore getFormula() {
        return formula;
    }

    public void setFormula(FormulaScore formula) {
        this.formula = formula;
    }

    public VariableRiesgo getVariable() {
        return variable;
    }

    public void setVariable(VariableRiesgo variable) {
        this.variable = variable;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }
}
