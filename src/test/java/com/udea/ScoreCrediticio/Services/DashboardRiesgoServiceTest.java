package com.udea.ScoreCrediticio.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO;
import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO.ConteoNivelRiesgo;
import com.udea.ScoreCrediticio.DTOs.Response.DistribucionRiesgoResponseDTO;
import com.udea.ScoreCrediticio.Exceptions.RangoFechasInvalidoException;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;

class DashboardRiesgoServiceTest {

    private HistorialEvaluacionDAO historialEvaluacionDAO;
    private CarteraConsultaService carteraConsultaService;
    private DashboardRiesgoService dashboardRiesgoService;

    @BeforeEach
    void setUp() {
        historialEvaluacionDAO = mock(HistorialEvaluacionDAO.class);
        carteraConsultaService = new CarteraConsultaService(historialEvaluacionDAO);
        dashboardRiesgoService = new DashboardRiesgoService(carteraConsultaService);
    }

    @Test
    void distribucionCalculaTotalesYPorcentajesParaLosTresNiveles() {
        when(historialEvaluacionDAO.contarUltimaEvaluacionPorNivelEnRango(any(), any()))
                .thenReturn(List.of(
                        conteo(NivelRiesgo.BAJO, 2L),
                        conteo(NivelRiesgo.ALTO, 1L)));

        DistribucionRiesgoResponseDTO resultado = dashboardRiesgoService.obtenerDistribucion(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(3L, resultado.getTotalSolicitantes());
        assertEquals(DashboardRiesgoService.MENSAJE_CON_DATOS, resultado.getMensaje());
        assertEquals(3, resultado.getDistribucion().size());

        assertEquals(NivelRiesgo.BAJO, resultado.getDistribucion().get(0).getNivel());
        assertEquals(2L, resultado.getDistribucion().get(0).getTotal());
        assertEquals(new BigDecimal("66.67"), resultado.getDistribucion().get(0).getPorcentaje());

        assertEquals(NivelRiesgo.MEDIO, resultado.getDistribucion().get(1).getNivel());
        assertEquals(0L, resultado.getDistribucion().get(1).getTotal());
        assertEquals(new BigDecimal("0.00"), resultado.getDistribucion().get(1).getPorcentaje());

        assertEquals(NivelRiesgo.ALTO, resultado.getDistribucion().get(2).getNivel());
        assertEquals(1L, resultado.getDistribucion().get(2).getTotal());
        assertEquals(new BigDecimal("33.33"), resultado.getDistribucion().get(2).getPorcentaje());
    }

    @Test
    void periodoSinEvaluacionesDevuelveCerosYMensajeExacto() {
        when(historialEvaluacionDAO.contarUltimaEvaluacionPorNivelEnRango(any(), any()))
                .thenReturn(List.of());

        DistribucionRiesgoResponseDTO resultado = dashboardRiesgoService.obtenerDistribucion(null, null);

        assertEquals(0L, resultado.getTotalSolicitantes());
        assertEquals("No se registraron evaluaciones de riesgo en el periodo seleccionado",
                resultado.getMensaje());
        assertEquals(3, resultado.getDistribucion().size());
        resultado.getDistribucion().forEach(categoria -> {
            assertEquals(0L, categoria.getTotal());
            assertEquals(new BigDecimal("0.00"), categoria.getPorcentaje());
        });
    }

    @Test
    void fechaInicioPosteriorAFechaFinLanzaExcepcion() {
        assertThrows(RangoFechasInvalidoException.class,
                () -> dashboardRiesgoService.obtenerDistribucion(
                        LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)));
    }

    private ConteoNivelRiesgo conteo(NivelRiesgo nivel, long total) {
        return new ConteoNivelRiesgo() {
            @Override
            public NivelRiesgo getNivel() {
                return nivel;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }
}
