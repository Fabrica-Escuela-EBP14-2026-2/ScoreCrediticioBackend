package com.udea.ScoreCrediticio.Services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DAOs.RangoRiesgoDAO;
import com.udea.ScoreCrediticio.DTOs.Request.DefinirRangosRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Request.RangoItemRequestDTO;
import com.udea.ScoreCrediticio.DTOs.Response.ClasificacionRiesgoResponseDTO;
import com.udea.ScoreCrediticio.DTOs.Response.EscalaRiesgoResponseDTO;
import com.udea.ScoreCrediticio.DTOs.Response.RangoItemResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.BrechaEnEscalaException;
import com.udea.ScoreCrediticio.Exceptions.RangoInvalidoException;
import com.udea.ScoreCrediticio.Exceptions.RangosSolapadosException;
import com.udea.ScoreCrediticio.Exceptions.ResourceNotFoundException;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;
import com.udea.ScoreCrediticio.Model.RangoRiesgo;

/**
 * HU09 - Configuracion de rangos y niveles de riesgo.
 * Define la escala de puntajes (0-1000) que clasifica la capacidad crediticia de un
 * solicitante en los niveles Bajo, Medio y Alto. La escala debe ser continua: sin
 * solapamientos entre rangos y sin valores de puntaje sin cobertura.
 */
@Service
public class RangoRiesgoService {

    private static final int PUNTAJE_MINIMO_ESCALA = 0;
    private static final int PUNTAJE_MAXIMO_ESCALA = 1000;

    private static final String MENSAJE_GUARDADO = "Escala de niveles de riesgo guardada con \u00e9xito";
    private static final String MENSAJE_VIGENTE = "Escala de niveles de riesgo vigente";
    private static final String MENSAJE_SOLAPE = "Los rangos num\u00e9ricos no pueden solaparse entre s\u00ed";
    private static final String MENSAJE_SIN_ESCALA = "No hay una escala de niveles de riesgo configurada";

    private final RangoRiesgoDAO rangoRiesgoDAO;

    public RangoRiesgoService(RangoRiesgoDAO rangoRiesgoDAO) {
        this.rangoRiesgoDAO = rangoRiesgoDAO;
    }

    /**
     * Guarda una escala de niveles de riesgo completa y continua.
     * Valida los limites de cada rango, luego los solapamientos, despues las brechas y
     * por ultimo la cobertura de los tres niveles. Asi, aunque la pantalla envie solo
     * algunos niveles, el mensaje especifico del escenario (solape o brecha) prevalece.
     * SERIALIZABLE evita que dos guardados concurrentes dejen la escala inconsistente.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EscalaRiesgoResponseDTO guardarRangos(DefinirRangosRequestDTO dto) {
        List<RangoItemRequestDTO> rangos = dto.getRangos();

        validarLimites(rangos);

        List<RangoItemRequestDTO> ordenados = rangos.stream()
                .sorted((a, b) -> Integer.compare(a.getMin(), b.getMin()))
                .toList();

        validarSinSolapamientos(ordenados);
        validarSinBrechas(ordenados);
        validarNiveles(rangos);

        // Reemplazo atomico de la escala: solo existe una escala vigente a la vez.
        rangoRiesgoDAO.deleteAllInBatch();

        List<RangoRiesgo> nuevas = rangos.stream()
                .map(r -> new RangoRiesgo(r.getNivel(), r.getMin(), r.getMax()))
                .toList();
        List<RangoRiesgo> guardadas = rangoRiesgoDAO.saveAll(nuevas);

        return new EscalaRiesgoResponseDTO(MENSAJE_GUARDADO, mapear(guardadas));
    }

    @Transactional(readOnly = true)
    public EscalaRiesgoResponseDTO obtenerEscala() {
        List<RangoRiesgo> rangos = rangoRiesgoDAO.findAllByOrderByPuntajeMinimoDesc();
        if (rangos.isEmpty()) {
            throw new ResourceNotFoundException(MENSAJE_SIN_ESCALA);
        }
        return new EscalaRiesgoResponseDTO(MENSAJE_VIGENTE, mapear(rangos));
    }

    /**
     * Clasifica un puntaje (0-1000) en el nivel de riesgo que le corresponde segun
     * la escala vigente. Es el objetivo de la historia: clasificar automaticamente
     * la capacidad crediticia a partir de la puntuacion obtenida.
     */
    @Transactional(readOnly = true)
    public ClasificacionRiesgoResponseDTO clasificar(int puntaje) {
        if (puntaje < PUNTAJE_MINIMO_ESCALA || puntaje > PUNTAJE_MAXIMO_ESCALA) {
            throw new RangoInvalidoException(
                    "El puntaje a clasificar debe estar entre " + PUNTAJE_MINIMO_ESCALA + " y "
                            + PUNTAJE_MAXIMO_ESCALA + " puntos");
        }

        List<RangoRiesgo> rangos = rangoRiesgoDAO.findAll();
        if (rangos.isEmpty()) {
            throw new ResourceNotFoundException(MENSAJE_SIN_ESCALA);
        }

        NivelRiesgo nivel = rangos.stream()
                .filter(r -> puntaje >= r.getPuntajeMinimo() && puntaje <= r.getPuntajeMaximo())
                .map(RangoRiesgo::getNivel)
                .findFirst()
                .orElseThrow(() -> new BrechaEnEscalaException(
                        "El puntaje " + puntaje + " no esta cubierto por la escala de niveles de riesgo vigente",
                        List.of(String.valueOf(puntaje))));

        String mensaje = "Puntaje " + puntaje + " clasificado como riesgo " + nivel.getEtiqueta();
        return new ClasificacionRiesgoResponseDTO(puntaje, nivel, mensaje);
    }

    private void validarNiveles(List<RangoItemRequestDTO> rangos) {
        Set<NivelRiesgo> vistos = new LinkedHashSet<>();
        for (RangoItemRequestDTO r : rangos) {
            if (r.getNivel() == null) {
                throw new RangoInvalidoException("El nivel de riesgo es obligatorio en cada rango");
            }
            if (!vistos.add(r.getNivel())) {
                throw new RangoInvalidoException(
                        "El nivel de riesgo '" + r.getNivel().getEtiqueta() + "' solo puede definirse una vez");
            }
        }

        for (NivelRiesgo nivel : NivelRiesgo.values()) {
            if (!vistos.contains(nivel)) {
                throw new RangoInvalidoException(
                        "La escala debe incluir los tres niveles de riesgo (Bajo, Medio y Alto). Falta: "
                                + nivel.getEtiqueta());
            }
        }
    }

    private void validarLimites(List<RangoItemRequestDTO> rangos) {
        for (RangoItemRequestDTO r : rangos) {
            if (r.getNivel() == null) {
                throw new RangoInvalidoException("El nivel de riesgo es obligatorio en cada rango");
            }
            if (r.getMin() == null || r.getMax() == null) {
                throw new RangoInvalidoException(
                        "El rango del nivel '" + etiqueta(r) + "' debe tener puntaje minimo y maximo");
            }
            if (r.getMin() < PUNTAJE_MINIMO_ESCALA || r.getMax() > PUNTAJE_MAXIMO_ESCALA) {
                throw new RangoInvalidoException(
                        "El rango del nivel '" + etiqueta(r) + "' debe estar entre " + PUNTAJE_MINIMO_ESCALA
                                + " y " + PUNTAJE_MAXIMO_ESCALA + " puntos");
            }
            if (r.getMin() > r.getMax()) {
                throw new RangoInvalidoException(
                        "El rango del nivel '" + etiqueta(r)
                                + "' tiene el puntaje minimo por encima del maximo (" + r.getMin() + " - " + r.getMax()
                                + ")");
            }
        }
    }

    private void validarSinSolapamientos(List<RangoItemRequestDTO> ordenados) {
        Set<String> camposConflicto = new LinkedHashSet<>();
        Set<String> nivelesConflicto = new LinkedHashSet<>();

        for (int i = 1; i < ordenados.size(); i++) {
            RangoItemRequestDTO anterior = ordenados.get(i - 1);
            RangoItemRequestDTO actual = ordenados.get(i);

            if (actual.getMin() <= anterior.getMax()) {
                camposConflicto.add(anterior.getNivel().getEtiqueta() + "-max");
                camposConflicto.add(actual.getNivel().getEtiqueta() + "-min");
                nivelesConflicto.add(anterior.getNivel().getEtiqueta());
                nivelesConflicto.add(actual.getNivel().getEtiqueta());
            }
        }

        if (!camposConflicto.isEmpty()) {
            throw new RangosSolapadosException(
                    MENSAJE_SOLAPE,
                    new ArrayList<>(camposConflicto),
                    new ArrayList<>(nivelesConflicto));
        }
    }

    private void validarSinBrechas(List<RangoItemRequestDTO> ordenados) {
        List<String> brechas = new ArrayList<>();

        RangoItemRequestDTO primero = ordenados.get(0);
        if (primero.getMin() > PUNTAJE_MINIMO_ESCALA) {
            brechas.add(rangoTexto(PUNTAJE_MINIMO_ESCALA, primero.getMin() - 1));
        }

        for (int i = 1; i < ordenados.size(); i++) {
            int inicioBrecha = ordenados.get(i - 1).getMax() + 1;
            int finBrecha = ordenados.get(i).getMin() - 1;
            if (inicioBrecha <= finBrecha) {
                brechas.add(rangoTexto(inicioBrecha, finBrecha));
            }
        }

        RangoItemRequestDTO ultimo = ordenados.get(ordenados.size() - 1);
        if (ultimo.getMax() < PUNTAJE_MAXIMO_ESCALA) {
            brechas.add(rangoTexto(ultimo.getMax() + 1, PUNTAJE_MAXIMO_ESCALA));
        }

        if (!brechas.isEmpty()) {
            throw new BrechaEnEscalaException(
                    "La escala de niveles de riesgo deja puntajes sin clasificar. Intervalos sin cobertura: "
                            + String.join(", ", brechas),
                    brechas);
        }
    }

    private List<RangoItemResponseDTO> mapear(List<RangoRiesgo> rangos) {
        return rangos.stream()
                .sorted((a, b) -> Integer.compare(b.getPuntajeMinimo(), a.getPuntajeMinimo()))
                .map(r -> new RangoItemResponseDTO(r.getNivel(), r.getPuntajeMinimo(), r.getPuntajeMaximo()))
                .toList();
    }

    private String rangoTexto(int min, int max) {
        return min + "-" + max;
    }

    private String etiqueta(RangoItemRequestDTO r) {
        return r.getNivel() == null ? "Sin nivel" : r.getNivel().getEtiqueta();
    }
}
