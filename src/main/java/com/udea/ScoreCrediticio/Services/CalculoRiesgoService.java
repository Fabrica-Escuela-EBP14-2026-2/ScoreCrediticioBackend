package com.udea.ScoreCrediticio.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO;
import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.DTOs.Request.CalcularRiesgoRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Request.ValorVariableRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.ResultadoRiesgoResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.CalculoRiesgoInvalidoException;
import com.udea.ScoreCrediticio.Exceptions.ResourceNotFoundException;
import com.udea.ScoreCrediticio.Model.FormulaScore;
import com.udea.ScoreCrediticio.Model.HistorialEvaluacion;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;
import com.udea.ScoreCrediticio.Model.PonderacionVariable;
import com.udea.ScoreCrediticio.Model.Solicitante;

/**
 * HU10 - Calculo del riesgo crediticio de un solicitante.
 * Aplica la formula vigente (pesos de HU08) sobre los valores 0-100 que la ficha
 * envia por cada variable activa, obtiene el puntaje 0-1000, lo clasifica con la
 * escala vigente (HU09) y persiste el resultado en el historial.
 */
@Service
public class CalculoRiesgoService {

    private static final BigDecimal DIEZ = BigDecimal.TEN;

    private final SolicitanteDAO solicitanteDAO;
    private final HistorialEvaluacionDAO historialEvaluacionDAO;
    private final FormulaScoreService formulaScoreService;
    private final RangoRiesgoService rangoRiesgoService;

    public CalculoRiesgoService(SolicitanteDAO solicitanteDAO,
            HistorialEvaluacionDAO historialEvaluacionDAO,
            FormulaScoreService formulaScoreService,
            RangoRiesgoService rangoRiesgoService) {
        this.solicitanteDAO = solicitanteDAO;
        this.historialEvaluacionDAO = historialEvaluacionDAO;
        this.formulaScoreService = formulaScoreService;
        this.rangoRiesgoService = rangoRiesgoService;
    }

    /**
     * Ejecuta el calculo completo: valida el solicitante y la formula vigente,
     * verifica que los valores cubran exactamente las variables de la formula,
     * obtiene el puntaje ponderado (0-1000), lo clasifica y guarda el historial.
     */
    @Transactional
    public ResultadoRiesgoResponseDTO calcularRiesgo(Long solicitanteId, CalcularRiesgoRequestDTO dto) {
        if (dto.getValores() == null || dto.getValores().isEmpty()) {
            throw new CalculoRiesgoInvalidoException(
                    "La lista de valores de las variables no puede estar vacía");
        }

        Solicitante solicitante = solicitanteDAO.findById(solicitanteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitante con id " + solicitanteId + " no encontrado"));

        FormulaScore formula = formulaScoreService.obtenerFormulaVigenteValidada();

        Map<Long, BigDecimal> valoresPorVariable = validarCoberturaDeVariables(formula, dto.getValores());
        int puntaje = calcularPuntaje(formula, valoresPorVariable);
        NivelRiesgo nivel = rangoRiesgoService.determinarNivel(puntaje);

        // saveAndFlush y no save: @CreationTimestamp se asigna en el INSERT, y sin flush
        // la respuesta devolveria fechaCalculo en null.
        HistorialEvaluacion historial = historialEvaluacionDAO.saveAndFlush(
                new HistorialEvaluacion(solicitante, puntaje, nivel));

        String mensaje = "Riesgo calculado: puntaje " + puntaje + ", nivel " + nivel.getEtiqueta();
        return new ResultadoRiesgoResponseDTO(
                solicitante.getId(),
                historial.getPuntaje(),
                historial.getNivel(),
                historial.getFechaCalculo(),
                mensaje);
    }

    /**
     * Puntaje = suma de cada valor (0-100) multiplicado por el peso de su variable,
     * dividida entre 10. Equivale a Σ(valor × peso/100) × 10, el rango 0-1000 que
     * maneja la escala de niveles de riesgo (HU09). Se redondea una sola vez, al final.
     */
    private int calcularPuntaje(FormulaScore formula, Map<Long, BigDecimal> valoresPorVariable) {
        BigDecimal sumaPonderada = BigDecimal.ZERO;
        for (PonderacionVariable ponderacion : formula.getPonderaciones()) {
            BigDecimal valor = valoresPorVariable.get(ponderacion.getVariable().getId());
            sumaPonderada = sumaPonderada.add(valor.multiply(ponderacion.getPeso()));
        }
        return sumaPonderada.divide(DIEZ, 0, RoundingMode.HALF_UP).intValueExact();
    }

    /**
     * La formula vigente define exactamente las variables que se deben evaluar, asi que
     * los valores enviados deben cubrirla sin faltantes, sin variables ajenas y sin
     * ids repetidos.
     */
    private Map<Long, BigDecimal> validarCoberturaDeVariables(FormulaScore formula,
            List<ValorVariableRequestDTO> valores) {
        Map<Long, BigDecimal> valoresPorVariable = new LinkedHashMap<>();
        for (ValorVariableRequestDTO item : valores) {
            if (valoresPorVariable.putIfAbsent(item.getVariableId(), item.getValor()) != null) {
                throw new CalculoRiesgoInvalidoException(
                        "La variable con id " + item.getVariableId() + " aparece más de una vez en los valores enviados");
            }
        }

        Set<Long> idsFormula = formula.getPonderaciones().stream()
                .map(p -> p.getVariable().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> faltantes = formula.getPonderaciones().stream()
                .filter(p -> !valoresPorVariable.containsKey(p.getVariable().getId()))
                .map(p -> p.getVariable().getNombre())
                .toList();
        if (!faltantes.isEmpty()) {
            throw new CalculoRiesgoInvalidoException(
                    "Faltan valores para las variables requeridas por la fórmula vigente: "
                            + String.join(", ", faltantes));
        }

        List<String> ajenas = valoresPorVariable.keySet().stream()
                .filter(id -> !idsFormula.contains(id))
                .map(String::valueOf)
                .toList();
        if (!ajenas.isEmpty()) {
            throw new CalculoRiesgoInvalidoException(
                    "Las siguientes variables no pertenecen a la fórmula vigente: " + String.join(", ", ajenas));
        }

        return valoresPorVariable;
    }
}
