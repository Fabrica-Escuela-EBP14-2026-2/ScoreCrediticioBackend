package com.udea.ScoreCrediticio.Services;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.ScoreCrediticio.DTOs.Response.CarteraReporteFilaDTO;
import com.udea.ScoreCrediticio.Exceptions.FormatoReporteInvalidoException;
import com.udea.ScoreCrediticio.Exceptions.ResourceNotFoundException;
import com.udea.ScoreCrediticio.Services.CarteraConsultaService.VentanaFechas;

/**
 * HU14 - Exportacion del reporte de cartera a formato descargable (CSV UTF-8).
 * Genera un archivo con solicitantes y sus evaluaciones de riesgo dentro del
 * periodo seleccionado.
 */
@Service
public class ReporteCarteraService {

    static final String MENSAJE_SIN_DATOS = "No existen datos disponibles para exportar con los criterios seleccionados";

    private static final String FORMATO_CSV = "CSV";
    private static final String NOMBRE_ARCHIVO_CSV = "reporte-cartera-riesgo.csv";
    private static final MediaType MEDIA_TYPE_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final byte[] UTF8_BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    private final CarteraConsultaService carteraConsultaService;

    public ReporteCarteraService(CarteraConsultaService carteraConsultaService) {
        this.carteraConsultaService = carteraConsultaService;
    }

    public record ArchivoExportado(byte[] contenido, MediaType contentType, String nombreArchivo) {
    }

    @Transactional(readOnly = true)
    public ArchivoExportado exportarCartera(LocalDate fechaInicio, LocalDate fechaFin, String formato) {
        validarFormato(formato);
        VentanaFechas ventana = carteraConsultaService.resolverVentanaFechas(fechaInicio, fechaFin);
        List<CarteraReporteFilaDTO> filas = carteraConsultaService.consultarEvaluacionesParaReporte(ventana);

        if (filas.isEmpty()) {
            throw new ResourceNotFoundException(MENSAJE_SIN_DATOS);
        }

        byte[] contenidoCsv = construirCsv(filas);
        return new ArchivoExportado(contenidoCsv, MEDIA_TYPE_CSV, NOMBRE_ARCHIVO_CSV);
    }

    private void validarFormato(String formato) {
        if (formato == null || formato.isBlank()) {
            return;
        }
        if (!FORMATO_CSV.equalsIgnoreCase(formato.trim())) {
            throw new FormatoReporteInvalidoException(
                    "Formato de exportación no soportado: " + formato + ". Formato permitido: CSV");
        }
    }

    private byte[] construirCsv(List<CarteraReporteFilaDTO> filas) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tipo Documento,Numero Documento,Nombre,Apellido,Fecha Evaluacion,Puntaje,Nivel Riesgo\r\n");

        for (CarteraReporteFilaDTO fila : filas) {
            sb.append(escaparCsv(fila.getTipoDocumento() == null ? null : fila.getTipoDocumento().name()))
                    .append(',')
                    .append(escaparCsv(fila.getNumeroDocumento()))
                    .append(',')
                    .append(escaparCsv(fila.getNombre()))
                    .append(',')
                    .append(escaparCsv(fila.getApellido()))
                    .append(',')
                    .append(escaparCsv(fila.getFechaEvaluacion() == null
                            ? null
                            : FORMATO_FECHA.format(fila.getFechaEvaluacion())))
                    .append(',')
                    .append(fila.getPuntaje() == null ? "" : fila.getPuntaje())
                    .append(',')
                    .append(escaparCsv(fila.getNivel() == null ? null : fila.getNivel().getEtiqueta()))
                    .append("\r\n");
        }

        byte[] cuerpo = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] resultado = new byte[UTF8_BOM.length + cuerpo.length];
        System.arraycopy(UTF8_BOM, 0, resultado, 0, UTF8_BOM.length);
        System.arraycopy(cuerpo, 0, resultado, UTF8_BOM.length, cuerpo.length);
        return resultado;
    }

    /**
     * Escapa comillas/comas/saltos segun RFC 4180 y neutraliza prefijos de
     * formulas para evitar CSV injection al abrir el archivo en hojas de calculo.
     */
    private String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }

        String seguro = valor;
        if (!seguro.isEmpty()) {
            char primero = seguro.charAt(0);
            if (primero == '=' || primero == '+' || primero == '-' || primero == '@'
                    || primero == '\t' || primero == '\r') {
                seguro = "'" + seguro;
            }
        }

        boolean requiereComillas = seguro.contains(",") || seguro.contains("\"")
                || seguro.contains("\n") || seguro.contains("\r");
        if (requiereComillas) {
            return "\"" + seguro.replace("\"", "\"\"") + "\"";
        }
        return seguro;
    }
}
