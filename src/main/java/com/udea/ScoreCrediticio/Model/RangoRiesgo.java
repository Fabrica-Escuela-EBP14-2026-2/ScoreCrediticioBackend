package com.udea.ScoreCrediticio.Model;

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
import jakarta.persistence.UniqueConstraint;

/**
 * Rango de puntaje asociado a un nivel de riesgo (HU09).
 * Solo puede existir una fila por nivel (restriccion UNIQUE en nivel), de modo que
 * una escala vigente esta compuesta exactamente por los tres niveles Bajo, Medio y Alto.
 * Los limites min/max son inclusivos y la escala completa debe cubrir de 0 a 1000 puntos.
 */
@Entity
@Table(name = "rango_riesgo", uniqueConstraints = @UniqueConstraint(name = "uk_rango_riesgo_nivel", columnNames = "nivel"))
public class RangoRiesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 10)
    private NivelRiesgo nivel;

    @Column(name = "puntaje_minimo", nullable = false)
    private Integer puntajeMinimo;

    @Column(name = "puntaje_maximo", nullable = false)
    private Integer puntajeMaximo;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public RangoRiesgo() {
    }

    public RangoRiesgo(NivelRiesgo nivel, Integer puntajeMinimo, Integer puntajeMaximo) {
        this.nivel = nivel;
        this.puntajeMinimo = puntajeMinimo;
        this.puntajeMaximo = puntajeMaximo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }

    public void setNivel(NivelRiesgo nivel) {
        this.nivel = nivel;
    }

    public Integer getPuntajeMinimo() {
        return puntajeMinimo;
    }

    public void setPuntajeMinimo(Integer puntajeMinimo) {
        this.puntajeMinimo = puntajeMinimo;
    }

    public Integer getPuntajeMaximo() {
        return puntajeMaximo;
    }

    public void setPuntajeMaximo(Integer puntajeMaximo) {
        this.puntajeMaximo = puntajeMaximo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
