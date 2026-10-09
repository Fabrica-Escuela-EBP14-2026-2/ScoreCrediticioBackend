package com.udea.ScoreCrediticio.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DAOs.FormulaScoreDAO;
import com.udea.ScoreCrediticio.DAOs.VariableRiesgoDAO;
import com.udea.ScoreCrediticio.DTOs.Request.DefinirFormulaRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Request.PonderacionItemRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.FormulaResponseDTO;
import com.udea.ScoreCrediticio.DTOs.Response.PonderacionItemResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.FormulaInvalidaException;
import com.udea.ScoreCrediticio.Exceptions.ResourceNotFoundException;
import com.udea.ScoreCrediticio.Exceptions.SumaPesosInvalidaException;
import com.udea.ScoreCrediticio.Model.EstadoVariable;
import com.udea.ScoreCrediticio.Model.FormulaScore;
import com.udea.ScoreCrediticio.Model.PonderacionVariable;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

@Service
public class FormulaScoreService {

    private static final BigDecimal PESO_OBJETIVO = new BigDecimal("100.0000");
    private static final int ESCALA_PESO = 4;

    private final FormulaScoreDAO formulaScoreDAO;
    private final VariableRiesgoDAO variableRiesgoDAO;

    public FormulaScoreService(FormulaScoreDAO formulaScoreDAO, VariableRiesgoDAO variableRiesgoDAO) {
        this.formulaScoreDAO = formulaScoreDAO;
        this.variableRiesgoDAO = variableRiesgoDAO;
    }

    /**
     * Define y persiste una nueva formula de scoring.
     * Valida que las ponderaciones cubran exactamente todas las variables activas,
     * no incluya inactivas ni duplicadas, y que la suma sea exactamente 100.0000%.
     * Utiliza aislamiento SERIALIZABLE para evitar condiciones de carrera entre administradores.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FormulaResponseDTO guardarFormula(DefinirFormulaRequestDTO dto) {
        if (dto.getPonderaciones() == null || dto.getPonderaciones().isEmpty()) {
            throw new FormulaInvalidaException("La lista de ponderaciones no puede estar vacía");
        }

        // 1. Evitar variables duplicadas en la peticion
        Set<Long> idsRecibidos = new HashSet<>();
        for (PonderacionItemRequestDTO item : dto.getPonderaciones()) {
            if (!idsRecibidos.add(item.getVariableId())) {
                throw new FormulaInvalidaException("No se permiten variables duplicadas en la fórmula. Id duplicado: " + item.getVariableId());
            }
        }

        // 2. Obtener todas las variables activas del catalogo
        List<VariableRiesgo> variablesActivas = variableRiesgoDAO.findAll().stream()
                .filter(v -> v.getEstado() == EstadoVariable.ACTIVA)
                .toList();

        if (variablesActivas.isEmpty()) {
            throw new FormulaInvalidaException("No hay variables de riesgo activas en el catálogo para conformar una fórmula");
        }

        // 3. Validar existencia y estado de cada variable enviada
        Map<Long, VariableRiesgo> mapaVariables = new HashMap<>();
        for (PonderacionItemRequestDTO item : dto.getPonderaciones()) {
            VariableRiesgo var = variableRiesgoDAO.findById(item.getVariableId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variable de riesgo con id " + item.getVariableId() + " no encontrada"));

            if (var.getEstado() != EstadoVariable.ACTIVA) {
                throw new FormulaInvalidaException("La variable '" + var.getNombre() + "' (id " + var.getId() + ") está inactiva y no puede incluirse en la fórmula vigente");
            }
            mapaVariables.put(var.getId(), var);
        }

        // 4. Exigir cobertura completa de variables activas
        Set<Long> idsActivas = variablesActivas.stream().map(VariableRiesgo::getId).collect(Collectors.toSet());
        if (!idsRecibidos.equals(idsActivas)) {
            Set<Long> faltantes = new HashSet<>(idsActivas);
            faltantes.removeAll(idsRecibidos);
            throw new FormulaInvalidaException("La fórmula debe ponderar todas las variables activas del catálogo. Faltan variables por incluir: " + faltantes);
        }

        // 5. Validar que la suma sea exactamente 100%
        BigDecimal totalCalculado = BigDecimal.ZERO.setScale(ESCALA_PESO);
        for (PonderacionItemRequestDTO item : dto.getPonderaciones()) {
            BigDecimal pesoItem = item.getPeso().setScale(ESCALA_PESO, RoundingMode.HALF_UP);
            totalCalculado = totalCalculado.add(pesoItem);
        }

        if (totalCalculado.compareTo(new BigDecimal("100")) != 0) {
            boolean esFaltante = totalCalculado.compareTo(new BigDecimal("100")) < 0;
            BigDecimal diferencia = esFaltante
                    ? new BigDecimal("100").subtract(totalCalculado).setScale(ESCALA_PESO, RoundingMode.HALF_UP)
                    : totalCalculado.subtract(new BigDecimal("100")).setScale(ESCALA_PESO, RoundingMode.HALF_UP);

            String tipoDescuadre = esFaltante ? "FALTANTE" : "EXCEDENTE";
            String mensaje = String.format("La suma de los pesos de la fórmula debe ser exactamente 100%%. Total calculado: %s. %s: %s",
                    formato(totalCalculado),
                    esFaltante ? "Faltante" : "Excedente",
                    formato(diferencia));

            throw new SumaPesosInvalidaException(mensaje, formato(totalCalculado), formato(diferencia), tipoDescuadre);
        }

        // 6. Transicion atomica: desactivar formula vigente anterior si existia
        formulaScoreDAO.findByVigenteTrue().ifPresent(formulaPrevia -> {
            formulaPrevia.setVigente(false);
            formulaPrevia.setClaveVigencia(null);
            formulaScoreDAO.saveAndFlush(formulaPrevia);
        });

        // 7. Persistir nueva formula marcada como vigente
        FormulaScore formula = new FormulaScore("Fórmula Global de Scoring", true, "VIGENTE", totalCalculado);
        for (PonderacionItemRequestDTO item : dto.getPonderaciones()) {
            VariableRiesgo var = mapaVariables.get(item.getVariableId());
            BigDecimal pesoItem = item.getPeso().setScale(ESCALA_PESO, RoundingMode.HALF_UP);
            formula.agregarPonderacion(new PonderacionVariable(formula, var, pesoItem));
        }

        FormulaScore guardada = formulaScoreDAO.saveAndFlush(formula);
        return mapearAResponse(guardada, "Fórmula de scoring guardada e implementada con éxito");
    }

    @Transactional(readOnly = true)
    public FormulaResponseDTO obtenerFormulaVigente() {
        FormulaScore formula = formulaScoreDAO.findByVigenteTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay una fórmula de scoring vigente en este momento"));

        // Si alguna variable de la formula vigente fue inactivada posteriormente, la formula ya no es completa
        boolean algunaInactiva = formula.getPonderaciones().stream()
                .anyMatch(p -> p.getVariable().getEstado() != EstadoVariable.ACTIVA);

        if (algunaInactiva) {
            throw new ResourceNotFoundException("No hay una fórmula de scoring vigente completa en este momento");
        }

        return mapearAResponse(formula, "Fórmula de scoring vigente");
    }

    /**
     * Inactiva la formula vigente si contiene una variable de riesgo que esta siendo desactivada,
     * garantizando que nunca quede una formula incompleta marcada como vigente.
     */
    @Transactional
    public void invalidarFormulaSiContieneVariable(Long variableId) {
        formulaScoreDAO.findByVigenteTrue().ifPresent(formula -> {
            boolean contiene = formula.getPonderaciones().stream()
                    .anyMatch(p -> p.getVariable().getId().equals(variableId));
            if (contiene) {
                formula.setVigente(false);
                formula.setClaveVigencia(null);
                formulaScoreDAO.saveAndFlush(formula);
            }
        });
    }

    private FormulaResponseDTO mapearAResponse(FormulaScore formula, String mensaje) {
        FormulaResponseDTO response = new FormulaResponseDTO();
        response.setId(formula.getId());
        response.setMensaje(mensaje);
        response.setEstado(formula.isVigente() ? "Vigente" : "Inactiva");
        response.setVigente(formula.isVigente());
        response.setSumaTotal(formula.getSumaTotal());
        response.setFechaImplementacion(formula.getFechaImplementacion());

        List<PonderacionItemResponseDTO> items = new ArrayList<>();
        for (PonderacionVariable p : formula.getPonderaciones()) {
            items.add(new PonderacionItemResponseDTO(
                    p.getVariable().getId(),
                    p.getVariable().getNombre(),
                    p.getVariable().getTipoDato(),
                    p.getPeso()
            ));
        }
        response.setPonderaciones(items);
        return response;
    }

    private String formato(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString() + "%";
    }
}
