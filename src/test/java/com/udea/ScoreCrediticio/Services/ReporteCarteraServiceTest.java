package com.udea.ScoreCrediticio.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO;
import com.udea.ScoreCrediticio.DTOs.Response.CarteraReporteFilaDTO;
import com.udea.ScoreCrediticio.Exceptions.FormatoReporteInvalidoException;
import com.udea.ScoreCrediticio.Exceptions.RangoFechasInvalidoException;
import com.udea.ScoreCrediticio.Exceptions.ResourceNotFoundException;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;
import com.udea.ScoreCrediticio.Model.TipoDocumento;
import com.udea.ScoreCrediticio.Services.ReporteCarteraService.ArchivoExportado;

class ReporteCarteraServiceTest {

    private HistorialEvaluacionDAO historialEvaluacionDAO;
    private ReporteCarteraService reporteCarteraService;

    @BeforeEach
    void setUp() {
        historialEvaluacionDAO = mock(HistorialEvaluacionDAO.class);
        CarteraConsultaService carteraConsultaService = new CarteraConsultaService(historialEvaluacionDAO);
        reporteCarteraService = new ReporteCarteraService(carteraConsultaService);
    }

    @Test
    void generaCsvConEncabezadosBomYProteccionDeInyeccion() {
        when(historialEvaluacionDAO.buscarEvaluacionesEnRango(any(), any()))
                .thenReturn(List.of(
                        new CarteraReporteFilaDTO(
                                TipoDocumento.CC,
                                "12345678",
                                "=CMD",
                                "Pérez, \"Jr\"",
                                LocalDateTime.of(2026, 10, 15, 14, 30, 0),
                                755,
                                NivelRiesgo.BAJO)));

        ArchivoExportado archivo = reporteCarteraService.exportarCartera(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "csv");

        String contenido = new String(archivo.contenido(), StandardCharsets.UTF_8);
        assertTrue(contenido.startsWith("\uFEFF"));
        String sinBom = contenido.substring(1);
        String[] lineas = sinBom.split("\r\n");

        assertEquals(2, lineas.length);
        assertEquals("Tipo Documento,Numero Documento,Nombre,Apellido,Fecha Evaluacion,Puntaje,Nivel Riesgo",
                lineas[0]);
        assertEquals("CC,12345678,'=CMD,\"Pérez, \"\"Jr\"\"\",2026-10-15 14:30:00,755,Bajo",
                lineas[1]);
    }

    @Test
    void sinDatosLanzaResourceNotFoundConMensajeExacto() {
        when(historialEvaluacionDAO.buscarEvaluacionesEnRango(any(), any()))
                .thenReturn(List.of());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> reporteCarteraService.exportarCartera(null, null, "CSV"));

        assertEquals("No existen datos disponibles para exportar con los criterios seleccionados",
                ex.getMessage());
    }

    @Test
    void formatoNoSoportadoLanzaExcepcion() {
        assertThrows(FormatoReporteInvalidoException.class,
                () -> reporteCarteraService.exportarCartera(null, null, "PDF"));
    }

    @Test
    void rangoFechasInvalidoLanzaExcepcion() {
        assertThrows(RangoFechasInvalidoException.class,
                () -> reporteCarteraService.exportarCartera(
                        LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10), "CSV"));
    }
}
