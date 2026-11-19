package com.udea.ScoreCrediticio.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO;
import com.udea.ScoreCrediticio.DAOs.PerfilFinancieroDAO;
import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.DTOs.Request.CalcularRiesgoRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Request.ValorVariableRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.ResultadoRiesgoResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.CalculoRiesgoInvalidoException;
import com.udea.ScoreCrediticio.Exceptions.DatosIncompletosException;
import com.udea.ScoreCrediticio.Exceptions.ResourceNotFoundException;
import com.udea.ScoreCrediticio.Model.FormulaScore;
import com.udea.ScoreCrediticio.Model.HistorialEvaluacion;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;
import com.udea.ScoreCrediticio.Model.PerfilFinanciero;
import com.udea.ScoreCrediticio.Model.PonderacionVariable;
import com.udea.ScoreCrediticio.Model.Solicitante;
import com.udea.ScoreCrediticio.Model.VariableEvaluada;

/**
 * HU10/HU11 - Calculo del riesgo crediticio de un solicitante.
 * Aplica la formula vigente (pesos de HU08) sobre los valores 0-100 que la ficha
 * envia por cada variable activa, obtiene el puntaje 0-1000, lo clasifica con la
 * escala vigente (HU09) y persiste el resultado en el historial.
 *
 * HU11 agrega el manejo de informacion faltante: si el solicitante no tiene sus
 * datos obligatorios o la peticion no cubre todas las variables de la formula, la
 * evaluacion se detiene y se informan los campos y variables faltantes.
 *
 * HU12 agrega el snapshot del evaluador, del perfil financiero y de las variables
 * usadas, para que el historial conserve los valores vigentes al momento del calculo.
 */
@Service
public class CalculoRiesgoService {

    private static final BigDecimal DIEZ = BigDecimal.TEN;
    private static final String MENSAJE_DATOS_INCOMPLETOS =
            "No es posible calcular el riesgo crediticio: el solicitante no cuenta con la información completa requerida para la evaluación";

    private final SolicitanteDAO solicitanteDAO;
    private final PerfilFinancieroDAO perfilFinancieroDAO;
    private final HistorialEvaluacionDAO historialEvaluacionDAO;
    private final FormulaScoreService formulaScoreService;
    private final RangoRiesgoService rangoRiesgoService;

    public CalculoRiesgoService(SolicitanteDAO solicitanteDAO,
            PerfilFinancieroDAO perfilFinancieroDAO,
            HistorialEvaluacionDAO historialEvaluacionDAO,
            FormulaScoreService formulaScoreService,
            RangoRiesgoService rangoRiesgoService) {
        this.solicitanteDAO = solicitanteDAO;
        this.perfilFinancieroDAO = perfilFinancieroDAO;
        this.historialEvaluacionDAO = historialEvaluacionDAO;
        this.formulaScoreService = formulaScoreService;
        this.rangoRiesgoService = rangoRiesgoService;
    }

    /**
     * Ejecuta el calculo completo: valida el solicitante y la formula vigente,
     * verifica que sus datos obligatorios y los valores de las variables esten
     * completos, obtiene el puntaje ponderado (0-1000), lo clasifica y guarda
     * el historial.
     */
    @Transactional
    public ResultadoRiesgoResponseDTO calcularRiesgo(Long solicitanteId, CalcularRiesgoRequestDTO dto,
            String evaluadorEmail) {
        Solicitante solicitante = solicitanteDAO.findById(solicitanteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitante con id " + solicitanteId + " no encontrado"));

        // La formula se lee en cada peticion para usar siempre las reglas vigentes.
        FormulaScore formula = formulaScoreService.obtenerFormulaVigenteParaCalculo();

        List<String> camposFaltantes = obtenerCamposFaltantes(solicitante);
        List<ValorVariableRequestDTO> valores = dto == null ? null : dto.getValores();
        Map<Long, BigDecimal> valoresPorVariable = construirMapaValores(valores);
        List<String> variablesFaltantes = obtenerVariablesFaltantes(formula, valoresPorVariable);

        if (!camposFaltantes.isEmpty() || !variablesFaltantes.isEmpty()) {
            throw new DatosIncompletosException(
                    MENSAJE_DATOS_INCOMPLETOS, camposFaltantes, variablesFaltantes);
        }

        validarVariablesAjenas(formula, valoresPorVariable.keySet());

        int puntaje = calcularPuntaje(formula, valoresPorVariable);
        NivelRiesgo nivel = rangoRiesgoService.determinarNivel(puntaje);

        PerfilFinanciero perfil = perfilFinancieroDAO.findBySolicitanteId(solicitanteId).orElse(null);
        HistorialEvaluacion historial = new HistorialEvaluacion(
                solicitante,
                puntaje,
                nivel,
                evaluadorEmail,
                perfil == null ? null : perfil.getIngresos(),
                perfil == null ? null : perfil.getEgresos());
        registrarVariablesEvaluadas(historial, formula, valoresPorVariable);

        // saveAndFlush y no save: @CreationTimestamp se asigna en el INSERT, y sin flush
        // la respuesta devolveria fechaCalculo en null.
        historial = historialEvaluacionDAO.saveAndFlush(historial);

        String mensaje = "Riesgo calculado: puntaje " + puntaje + ", nivel " + nivel.getEtiqueta();
        return new ResultadoRiesgoResponseDTO(
                solicitante.getId(),
                historial.getPuntaje(),
                historial.getNivel(),
                historial.getFechaCalculo(),
                mensaje);
    }

    /**
     * Copia al historial el nombre, peso y valor de cada variable de la formula
     * vigente, para conservar el detalle exacto usado en el calculo.
     */
    private void registrarVariablesEvaluadas(HistorialEvaluacion historial, FormulaScore formula,
            Map<Long, BigDecimal> valoresPorVariable) {
        for (PonderacionVariable ponderacion : formula.getPonderaciones()) {
            BigDecimal valor = valoresPorVariable.get(ponderacion.getVariable().getId());
            historial.agregarVariable(new VariableEvaluada(
                    ponderacion.getVariable().getId(),
                    ponderacion.getVariable().getNombre(),
                    valor,
                    ponderacion.getPeso()));
        }
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
     * Devuelve las claves de los datos obligatorios del solicitante que estan
     * ausentes o en blanco. El frontend usa esta lista para resaltar los campos.
     */
    private List<String> obtenerCamposFaltantes(Solicitante solicitante) {
        List<String> faltantes = new ArrayList<>();

        if (solicitante.getTipoDocumento() == null) {
            faltantes.add("tipoDocumento");
        }
        if (esBlanco(solicitante.getNumeroDocumento())) {
            faltantes.add("numeroDocumento");
        }
        if (esBlanco(solicitante.getNombre())) {
            faltantes.add("nombre");
        }
        if (esBlanco(solicitante.getApellido())) {
            faltantes.add("apellido");
        }
        if (esBlanco(solicitante.getTelefono())) {
            faltantes.add("telefono");
        }
        if (esBlanco(solicitante.getEmail())) {
            faltantes.add("email");
        }

        return faltantes;
    }

    private boolean esBlanco(String valor) {
        return valor == null || valor.isBlank();
    }

    /**
     * Normaliza los valores enviados. Un valor null se conserva en el mapa para
     * reportar la variable como faltante; los ids repetidos se rechazan como
     * solicitud invalida.
     */
    private Map<Long, BigDecimal> construirMapaValores(List<ValorVariableRequestDTO> valores) {
        Map<Long, BigDecimal> valoresPorVariable = new LinkedHashMap<>();
        if (valores == null) {
            return valoresPorVariable;
        }

        for (ValorVariableRequestDTO item : valores) {
            if (item == null || item.getVariableId() == null) {
                throw new CalculoRiesgoInvalidoException(
                        "Cada valor enviado debe indicar el id de la variable");
            }
            if (valoresPorVariable.containsKey(item.getVariableId())) {
                throw new CalculoRiesgoInvalidoException(
                        "La variable con id " + item.getVariableId()
                                + " aparece más de una vez en los valores enviados");
            }
            valoresPorVariable.put(item.getVariableId(), item.getValor());
        }

        return valoresPorVariable;
    }

    /**
     * La formula vigente define exactamente las variables que se deben evaluar.
     * Toda variable sin valor (incluido null) se informa para que la ficha la resalte.
     */
    private List<String> obtenerVariablesFaltantes(FormulaScore formula,
            Map<Long, BigDecimal> valoresPorVariable) {
        return formula.getPonderaciones().stream()
                .filter(ponderacion -> valoresPorVariable.get(ponderacion.getVariable().getId()) == null)
                .map(ponderacion -> ponderacion.getVariable().getNombre())
                .toList();
    }

    /**
     * Los valores enviados deben pertenecer a la formula vigente; de lo contrario
     * la solicitud esta mal formada y no se puede calcular un puntaje oficial.
     */
    private void validarVariablesAjenas(FormulaScore formula, Set<Long> idsEnviados) {
        Set<Long> idsFormula = formula.getPonderaciones().stream()
                .map(ponderacion -> ponderacion.getVariable().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> ajenas = idsEnviados.stream()
                .filter(id -> !idsFormula.contains(id))
                .map(String::valueOf)
                .toList();
        if (!ajenas.isEmpty()) {
            throw new CalculoRiesgoInvalidoException(
                    "Las siguientes variables no pertenecen a la fórmula vigente: " + String.join(", ", ajenas));
        }
    }
}
