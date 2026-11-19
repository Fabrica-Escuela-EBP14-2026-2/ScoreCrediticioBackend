package com.udea.ScoreCrediticio.Auditoria;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.persister.entity.EntityPersister;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.udea.ScoreCrediticio.Model.FormulaScore;
import com.udea.ScoreCrediticio.Model.PonderacionVariable;
import com.udea.ScoreCrediticio.Model.RangoRiesgo;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

/**
 * Construye representaciones JSON estables de las entidades auditadas.
 * El snapshot "nuevo" se toma de la entidad; el "anterior" se toma del estado
 * que Hibernate conserva antes de ejecutar el UPDATE o DELETE.
 */
public class AuditoriaSnapshots {

    private final ObjectMapper objectMapper;

    public AuditoriaSnapshots(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serializar(Map<String, Object> snapshot) {
        if (snapshot == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JacksonException ex) {
            throw new IllegalStateException("No se pudo serializar el snapshot de auditoria", ex);
        }
    }

    public Map<String, Object> desdeEntidad(Object entidad) {
        if (entidad instanceof VariableRiesgo variable) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", variable.getId());
            snapshot.put("nombre", variable.getNombre());
            snapshot.put("tipoDato", variable.getTipoDato());
            snapshot.put("peso", variable.getPeso());
            snapshot.put("estado", variable.getEstado());
            snapshot.put("fechaRegistro", variable.getFechaRegistro());
            return snapshot;
        }

        if (entidad instanceof FormulaScore formula) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", formula.getId());
            snapshot.put("nombre", formula.getNombre());
            snapshot.put("vigente", formula.isVigente());
            snapshot.put("claveVigencia", formula.getClaveVigencia());
            snapshot.put("sumaTotal", formula.getSumaTotal());
            snapshot.put("fechaImplementacion", formula.getFechaImplementacion());
            snapshot.put("ponderaciones", ponderaciones(formula));
            return snapshot;
        }

        if (entidad instanceof RangoRiesgo rango) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", rango.getId());
            snapshot.put("nivel", rango.getNivel());
            snapshot.put("puntajeMinimo", rango.getPuntajeMinimo());
            snapshot.put("puntajeMaximo", rango.getPuntajeMaximo());
            snapshot.put("fechaRegistro", rango.getFechaRegistro());
            return snapshot;
        }

        return null;
    }

    public Map<String, Object> desdeEstado(EntityPersister persister, Object[] estado,
            Object id, Object entidad) {
        if (estado == null) {
            return null;
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", id);

        String[] nombres = persister.getPropertyNames();
        for (int i = 0; i < nombres.length && i < estado.length; i++) {
            String nombre = nombres[i];
            if ("id".equals(nombre) || "ponderaciones".equals(nombre)) {
                continue;
            }
            snapshot.put(nombre, estado[i]);
        }

        // La coleccion de ponderaciones no forma parte del estado plano del UPDATE,
        // pero en la formula anterior se conserva igual y completa la comparacion.
        if (entidad instanceof FormulaScore formula) {
            snapshot.put("ponderaciones", ponderaciones(formula));
        }

        return snapshot;
    }

    private List<Map<String, Object>> ponderaciones(FormulaScore formula) {
        if (formula.getPonderaciones() == null || !Hibernate.isInitialized(formula.getPonderaciones())) {
            return List.of();
        }

        return formula.getPonderaciones().stream()
                .map(this::snapshotPonderacion)
                .toList();
    }

    private Map<String, Object> snapshotPonderacion(PonderacionVariable ponderacion) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("variableId", ponderacion.getVariable().getId());
        snapshot.put("nombreVariable", ponderacion.getVariable().getNombre());
        snapshot.put("tipoDato", ponderacion.getVariable().getTipoDato());
        snapshot.put("peso", ponderacion.getPeso());
        return snapshot;
    }
}
