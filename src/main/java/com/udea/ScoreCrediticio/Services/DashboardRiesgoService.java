package com.udea.ScoreCrediticio.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO.ConteoNivelRiesgo;
import com.udea.ScoreCrediticio.DTOs.Response.DistribucionCategoriaRiesgoDTO;
import com.udea.ScoreCrediticio.DTOs.Response.DistribucionRiesgoResponseDTO;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;
import com.udea.ScoreCrediticio.Services.CarteraConsultaService.VentanaFechas;

/**
 * HU13 - Dashboard de distribucion de riesgo de cartera.
 * Calcula totales y porcentajes por nivel (Bajo, Medio, Alto) usando la ultima
 * evaluacion de cada solicitante dentro del periodo consultado.
 */
@Service
public class DashboardRiesgoService {

    static final String MENSAJE_CON_DATOS = "Distribución de riesgo consultada con éxito";
    static final String MENSAJE_SIN_DATOS = "No se registraron evaluaciones de riesgo en el periodo seleccionado";

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal CERO_PORCENTAJE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int ESCALA_PORCENTAJE = 2;

    private final CarteraConsultaService carteraConsultaService;

    public DashboardRiesgoService(CarteraConsultaService carteraConsultaService) {
        this.carteraConsultaService = carteraConsultaService;
    }

    @Transactional(readOnly = true)
    public DistribucionRiesgoResponseDTO obtenerDistribucion(LocalDate fechaInicio, LocalDate fechaFin) {
        VentanaFechas ventana = carteraConsultaService.resolverVentanaFechas(fechaInicio, fechaFin);
        List<ConteoNivelRiesgo> conteos = carteraConsultaService.contarUltimaEvaluacionPorNivel(ventana);

        Map<NivelRiesgo, Long> totalesPorNivel = new EnumMap<>(NivelRiesgo.class);
        for (NivelRiesgo nivel : NivelRiesgo.values()) {
            totalesPorNivel.put(nivel, 0L);
        }

        long totalSolicitantes = 0L;
        for (ConteoNivelRiesgo conteo : conteos) {
            if (conteo.getNivel() != null && conteo.getTotal() != null) {
                long cantidad = conteo.getTotal();
                totalesPorNivel.put(conteo.getNivel(), cantidad);
                totalSolicitantes += cantidad;
            }
        }

        List<DistribucionCategoriaRiesgoDTO> distribucion = new ArrayList<>();
        for (NivelRiesgo nivel : NivelRiesgo.values()) {
            long cantidad = totalesPorNivel.get(nivel);
            BigDecimal porcentaje = calcularPorcentaje(cantidad, totalSolicitantes);
            distribucion.add(new DistribucionCategoriaRiesgoDTO(nivel, cantidad, porcentaje));
        }

        String mensaje = totalSolicitantes == 0 ? MENSAJE_SIN_DATOS : MENSAJE_CON_DATOS;
        return new DistribucionRiesgoResponseDTO(
                ventana.fechaInicio(),
                ventana.fechaFin(),
                totalSolicitantes,
                distribucion,
                mensaje);
    }

    private BigDecimal calcularPorcentaje(long cantidad, long total) {
        if (total == 0 || cantidad == 0) {
            return CERO_PORCENTAJE;
        }
        return BigDecimal.valueOf(cantidad)
                .multiply(CIEN)
                .divide(BigDecimal.valueOf(total), ESCALA_PORCENTAJE, RoundingMode.HALF_UP);
    }
}
