package com.udea.ScoreCrediticio;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO;
import com.udea.ScoreCrediticio.DAOs.PerfilFinancieroDAO;
import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.Model.HistorialEvaluacion;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;
import com.udea.ScoreCrediticio.Model.Solicitante;
import com.udea.ScoreCrediticio.Model.TipoDocumento;
import com.udea.ScoreCrediticio.Model.VariableEvaluada;

@SpringBootTest
@AutoConfigureMockMvc
class ReporteCarteraIntegrationTest {

    private static final String ENDPOINT = "/api/reportes/cartera/exportar";
    private static final String MENSAJE_SIN_DATOS =
            "No existen datos disponibles para exportar con los criterios seleccionados";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SolicitanteDAO solicitanteDAO;

    @Autowired
    private PerfilFinancieroDAO perfilFinancieroDAO;

    @Autowired
    private HistorialEvaluacionDAO historialEvaluacionDAO;

    private String tokenAdmin;
    private String tokenAnalista;

    @BeforeEach
    void preparar() throws Exception {
        historialEvaluacionDAO.deleteAll();
        perfilFinancieroDAO.deleteAll();
        solicitanteDAO.deleteAll();

        tokenAdmin = obtenerToken("admin@score.local", "Admin123*");
        tokenAnalista = obtenerToken("analista@score.local", "Analista123*");
    }

    @Test
    void exportaCsvConSolicitantesYEvaluacionesEnElRangoSinDuplicados() throws Exception {
        Solicitante ana = crearSolicitante("12345678", "Ana", "Pérez");
        Solicitante luis = crearSolicitante("87654321", "Luis", "Gómez");

        guardarEvaluacion(ana, 250, NivelRiesgo.ALTO, LocalDateTime.of(2026, 9, 28, 9, 0));
        guardarEvaluacion(ana, 620, NivelRiesgo.MEDIO, LocalDateTime.of(2026, 10, 10, 14, 15, 30));
        guardarEvaluacion(ana, 810, NivelRiesgo.BAJO, LocalDateTime.of(2026, 10, 25, 16, 45, 0));
        guardarEvaluacion(luis, 430, NivelRiesgo.MEDIO, LocalDateTime.of(2026, 10, 18, 8, 5, 10));

        MvcResult result = mockMvc.perform(get(ENDPOINT)
                        .param("fechaInicio", "2026-10-01")
                        .param("fechaFin", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/csv")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=\"reporte-cartera-riesgo.csv\"")))
                .andReturn();

        String csv = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"));
        String[] lineas = csv.substring(1).split("\r\n");

        // Encabezado + 3 evaluaciones dentro de octubre (2 de Ana, 1 de Luis; septiembre queda fuera).
        assertEquals(4, lineas.length);
        assertEquals("Tipo Documento,Numero Documento,Nombre,Apellido,Fecha Evaluacion,Puntaje,Nivel Riesgo",
                lineas[0]);
        assertEquals("CC,12345678,Ana,Pérez,2026-10-25 16:45:00,810,Bajo", lineas[1]);
        assertEquals("CC,87654321,Luis,Gómez,2026-10-18 08:05:10,430,Medio", lineas[2]);
        assertEquals("CC,12345678,Ana,Pérez,2026-10-10 14:15:30,620,Medio", lineas[3]);
    }

    @Test
    void sinDatosParaExportarDevuelve404ConMensajeExacto() throws Exception {
        Solicitante sinEvaluar = crearSolicitante("99990001", "Laura", "Díaz");
        guardarEvaluacion(sinEvaluar, 750, NivelRiesgo.BAJO, LocalDateTime.of(2026, 8, 1, 10, 0));

        mockMvc.perform(get(ENDPOINT)
                        .param("fechaInicio", "2026-10-01")
                        .param("fechaFin", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value(MENSAJE_SIN_DATOS));
    }

    @Test
    void parametrosInvalidosDevuelven400() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("fechaInicio", "2026-10-20")
                        .param("fechaFin", "2026-10-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Rango de fechas inválido"));

        mockMvc.perform(get(ENDPOINT)
                        .param("formato", "xlsx")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Formato de reporte inválido"));
    }

    @Test
    void seguridadExigeTokenYRolAdministrador() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsExponeContentDispositionAlFrontend() throws Exception {
        mockMvc.perform(options(ENDPOINT)
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        containsString("Content-Disposition")));
    }

    private Solicitante crearSolicitante(String documento, String nombre, String apellido) {
        return solicitanteDAO.save(new Solicitante(
                null, nombre, apellido, "3001234567",
                nombre.toLowerCase() + "@test.com", documento, TipoDocumento.CC));
    }

    private void guardarEvaluacion(Solicitante solicitante, int puntaje, NivelRiesgo nivel,
            LocalDateTime fechaCalculo) {
        HistorialEvaluacion eval = new HistorialEvaluacion(
                solicitante, puntaje, nivel, "admin@score.local", null, null);
        // Dos variables por evaluacion para validar que no haya producto cartesiano en el reporte.
        eval.agregarVariable(new VariableEvaluada(1L, "Ingresos", new BigDecimal("80"), new BigDecimal("50")));
        eval.agregarVariable(new VariableEvaluada(2L, "Egresos", new BigDecimal("70"), new BigDecimal("50")));
        eval = historialEvaluacionDAO.saveAndFlush(eval);

        jdbcTemplate.update("UPDATE historial_evaluacion SET fecha_calculo = ? WHERE id = ?",
                Timestamp.valueOf(fechaCalculo), eval.getId());
    }

    private String obtenerToken(String email, String password) throws Exception {
        MockHttpServletRequestBuilder login = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        MvcResult result = mockMvc.perform(login).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
