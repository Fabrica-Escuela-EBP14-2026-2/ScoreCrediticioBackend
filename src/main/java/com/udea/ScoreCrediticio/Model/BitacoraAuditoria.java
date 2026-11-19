package com.udea.ScoreCrediticio.Model;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Registro inmutable de un cambio sobre las reglas del motor de scoring.
 * La escritura la realiza el listener de Hibernate directamente por JDBC dentro
 * de la misma transaccion; la entidad solo se usa para consultas.
 */
@Entity
@Table(name = "bitacora_auditoria", indexes = @Index(
        name = "idx_bitacora_auditoria_fecha",
        columnList = "fecha_utc, id"))
@Immutable
public class BitacoraAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "usuario", nullable = false, updatable = false, length = 255)
    private String usuario;

    @Column(name = "fecha_utc", nullable = false, updatable = false)
    private OffsetDateTime fechaUtc;

    @Enumerated(EnumType.STRING)
    @Column(name = "modulo", nullable = false, updatable = false, length = 50)
    private ModuloAuditoria modulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, updatable = false, length = 30)
    private AccionAuditoria accion;

    @Column(name = "valor_anterior", updatable = false, columnDefinition = "text")
    private String valorAnterior;

    @Column(name = "valor_nuevo", updatable = false, columnDefinition = "text")
    private String valorNuevo;

    public BitacoraAuditoria() {
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public OffsetDateTime getFechaUtc() {
        return fechaUtc;
    }

    public ModuloAuditoria getModulo() {
        return modulo;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }
}
