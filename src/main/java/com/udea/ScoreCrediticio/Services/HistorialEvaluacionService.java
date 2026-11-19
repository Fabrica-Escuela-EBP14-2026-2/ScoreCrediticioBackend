package com.udea.ScoreCrediticio.Services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO;
import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.DTOs.Response.HistorialEvaluacionResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.ResourceNotFoundException;
import com.udea.ScoreCrediticio.Mapper.HistorialEvaluacionMapper;

/**
 * HU12 - Consulta del historial de evaluaciones de un solicitante.
 * Devuelve las evaluaciones de la mas reciente a la mas antigua, con el
 * snapshot de datos financieros y variables de cada calculo.
 */
@Service
public class HistorialEvaluacionService {

    private final SolicitanteDAO solicitanteDAO;
    private final HistorialEvaluacionDAO historialEvaluacionDAO;
    private final HistorialEvaluacionMapper historialEvaluacionMapper;

    public HistorialEvaluacionService(SolicitanteDAO solicitanteDAO,
            HistorialEvaluacionDAO historialEvaluacionDAO,
            HistorialEvaluacionMapper historialEvaluacionMapper) {
        this.solicitanteDAO = solicitanteDAO;
        this.historialEvaluacionDAO = historialEvaluacionDAO;
        this.historialEvaluacionMapper = historialEvaluacionMapper;
    }

    @Transactional(readOnly = true)
    public List<HistorialEvaluacionResponseDTO> consultarHistorial(Long solicitanteId) {
        if (!solicitanteDAO.existsById(solicitanteId)) {
            throw new ResourceNotFoundException(
                    "Solicitante con id " + solicitanteId + " no encontrado");
        }

        return historialEvaluacionDAO
                .findBySolicitanteIdOrderByFechaCalculoDescIdDesc(solicitanteId)
                .stream()
                .map(historialEvaluacionMapper::toResponseDto)
                .toList();
    }
}
