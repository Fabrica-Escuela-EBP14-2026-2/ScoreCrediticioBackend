package com.udea.ScoreCrediticio.Controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.udea.ScoreCrediticio.Services.ReporteCarteraService;
import com.udea.ScoreCrediticio.Services.ReporteCarteraService.ArchivoExportado;

/**
 * HU14: exporta el reporte de cartera con solicitantes y evaluaciones de riesgo
 * en formato CSV descargable.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteCarteraController {

    private final ReporteCarteraService reporteCarteraService;

    public ReporteCarteraController(ReporteCarteraService reporteCarteraService) {
        this.reporteCarteraService = reporteCarteraService;
    }

    @GetMapping("/cartera/exportar")
    public ResponseEntity<byte[]> exportarCartera(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "CSV") String formato) {

        ArchivoExportado archivo = reporteCarteraService.exportarCartera(fechaInicio, fechaFin, formato);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(archivo.contentType());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(archivo.nombreArchivo(), StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl(CacheControl.noStore());

        return ResponseEntity.ok()
                .headers(headers)
                .body(archivo.contenido());
    }
}
