package com.udea.ScoreCrediticio.Auditoria;

import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import tools.jackson.databind.ObjectMapper;
import com.udea.ScoreCrediticio.Model.AccionAuditoria;
import com.udea.ScoreCrediticio.Model.FormulaScore;
import com.udea.ScoreCrediticio.Model.ModuloAuditoria;
import com.udea.ScoreCrediticio.Model.RangoRiesgo;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

/**
 * Listener de Hibernate que registra automaticamente los cambios de las reglas
 * del motor de scoring. La insercion usa la misma conexion de la sesion para que
 * la bitacora y el cambio de negocio se confirmen o reviertan juntos.
 */
public class AuditoriaEventListener implements PostInsertEventListener,
        PostUpdateEventListener, PostDeleteEventListener {

    private static final String INSERT_SQL = """
            INSERT INTO bitacora_auditoria
                (usuario, fecha_utc, modulo, accion, valor_anterior, valor_nuevo)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String USUARIO_SISTEMA = "SISTEMA";

    private final AuditoriaSnapshots snapshots;

    public AuditoriaEventListener(ObjectMapper objectMapper) {
        this.snapshots = new AuditoriaSnapshots(objectMapper);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (!esAuditable(event.getEntity())) {
            return;
        }

        registrar(event.getSession(), modulo(event.getEntity()), AccionAuditoria.CREAR,
                null, snapshots.desdeEntidad(event.getEntity()));
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (!esAuditable(event.getEntity())) {
            return;
        }

        registrar(event.getSession(), modulo(event.getEntity()), AccionAuditoria.ACTUALIZAR,
                snapshots.desdeEstado(event.getPersister(), event.getOldState(), event.getId(),
                        event.getEntity()),
                snapshots.desdeEntidad(event.getEntity()));
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (!esAuditable(event.getEntity())) {
            return;
        }

        registrar(event.getSession(), modulo(event.getEntity()), AccionAuditoria.ELIMINAR,
                snapshots.desdeEstado(event.getPersister(), event.getDeletedState(), event.getId(),
                        event.getEntity()),
                null);
    }

    private void registrar(SharedSessionContractImplementor session, ModuloAuditoria modulo,
            AccionAuditoria accion, Map<String, Object> anterior, Map<String, Object> nuevo) {

        String usuario = usuarioActual();
        String valorAnterior = snapshots.serializar(anterior);
        String valorNuevo = snapshots.serializar(nuevo);
        OffsetDateTime fechaUtc = OffsetDateTime.now(ZoneOffset.UTC);

        if (!(session instanceof SessionImplementor sessionImpl)) {
            throw new IllegalStateException(
                    "La sesion de Hibernate no permite el acceso JDBC requerido por la auditoria");
        }

        sessionImpl.runWithConnection((java.sql.Connection connection) -> {
            try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
                ps.setString(1, usuario);
                ps.setObject(2, fechaUtc);
                ps.setString(3, modulo.name());
                ps.setString(4, accion.name());
                ps.setString(5, valorAnterior);
                ps.setString(6, valorNuevo);
                ps.executeUpdate();
            }
        });
    }

    private boolean esAuditable(Object entidad) {
        return entidad instanceof VariableRiesgo
                || entidad instanceof FormulaScore
                || entidad instanceof RangoRiesgo;
    }

    private ModuloAuditoria modulo(Object entidad) {
        if (entidad instanceof VariableRiesgo) {
            return ModuloAuditoria.VARIABLE_RIESGO;
        }
        if (entidad instanceof FormulaScore) {
            return ModuloAuditoria.FORMULA_SCORE;
        }
        if (entidad instanceof RangoRiesgo) {
            return ModuloAuditoria.RANGO_RIESGO;
        }
        throw new IllegalArgumentException("Entidad no auditable: " + entidad.getClass().getName());
    }

    private String usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            return USUARIO_SISTEMA;
        }
        return autenticacion.getName();
    }
}
