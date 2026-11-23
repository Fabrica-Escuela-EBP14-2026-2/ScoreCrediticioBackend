package com.udea.ScoreCrediticio.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO;
import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO.ConteoNivelRiesgo;
import com.udea.ScoreCrediticio.DTOs.Response.CarteraReporteFilaDTO;
import com.udea.ScoreCrediticio.Exceptions.RangoFechasInvalidoException;

/**
 * Componente compartido para HU13 y HU14: centraliza la validacion de fechas
 * y expone las consultas especificas para el dashboard (ultima evaluacion por
 * solicitante) y el reporte descargable (evaluaciones en el rango).
 */
@Service
public class CarteraConsultaService {

    private static final LocalDateTime FECHA_MINIMA = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime FECHA_MAXIMA = LocalDateTime.of(9999, 12, 31, 0, 0);

    private final HistorialEvaluacionDAO historialEvaluacionDAO;

    public CarteraConsultaService(HistorialEvaluacionDAO historialEvaluacionDAO) {
        this.historialEvaluacionDAO = historialEvaluacionDAO;
    }

    public record VentanaFechas(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            LocalDateTime desde,
            LocalDateTime hastaExclusivo) {
    }

    /**
     * Valida que fechaInicio no sea posterior a fechaFin y convierte ambos extremos
     * a limites inclusivos de dia completo [desde, hastaExclusivo).
     */
    public VentanaFechas resolverVentanaFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new RangoFechasInvalidoException(
                    "La fecha de inicio (" + fechaInicio + ") no puede ser posterior a la fecha de fin ("
                            + fechaFin + ")");
        }

        LocalDateTime desde = (fechaInicio == null || fechaInicio.getYear() < 1900)
                ? FECHA_MINIMA
                : fechaInicio.atStartOfDay();

        LocalDateTime hastaExclusivo = (fechaFin == null || fechaFin.getYear() >= 9999)
                ? FECHA_MAXIMA
                : fechaFin.plusDays(1).atStartOfDay();

        return new VentanaFechas(fechaInicio, fechaFin, desde, hastaExclusivo);
    }

    @Transactional(readOnly = true)
    public List<ConteoNivelRiesgo> contarUltimaEvaluacionPorNivel(VentanaFechas ventana) {
        return historialEvaluacionDAO.contarUltimaEvaluacionPorNivelEnRango(
                ventana.desde(), ventana.hastaExclusivo());
    }

    @Transactional(readOnly = true)
    public List<CarteraReporteFilaDTO> consultarEvaluacionesParaReporte(VentanaFechas ventana) {
        return historialEvaluacionDAO.buscarEvaluacionesEnRango(
                ventana.desde(), ventana.hastaExclusivo());
    }
}
