package com.udea.ScoreCrediticio;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
class DashboardRiesgoIntegrationTest {

    private static final String ENDPOINT = "/api/dashboard/distribucion-riesgo";
    private static final String MENSAJE_SIN_DATOS =
            "No se registraron evaluaciones de riesgo en el periodo seleccionado";

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
    void distribucionNormalCuentaSolicitantesPorCategoria() throws Exception {
        Solicitante s1 = crearSolicitante("1001", "Ana", "Pérez");
        Solicitante s2 = crearSolicitante("1002", "Luis", "Gómez");
        Solicitante s3 = crearSolicitante("1003", "María", "López");
        Solicitante s4 = crearSolicitante("1004", "Carlos", "Ruiz");

        guardarEvaluacion(s1, 820, NivelRiesgo.BAJO, LocalDateTime.of(2026, 10, 5, 10, 0));
        guardarEvaluacion(s2, 750, NivelRiesgo.BAJO, LocalDateTime.of(2026, 10, 6, 11, 0));
        guardarEvaluacion(s3, 500, NivelRiesgo.MEDIO, LocalDateTime.of(2026, 10, 7, 12, 0));
        guardarEvaluacion(s4, 180, NivelRiesgo.ALTO, LocalDateTime.of(2026, 10, 8, 13, 0));

        mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSolicitantes").value(4))
                .andExpect(jsonPath("$.distribucion", hasSize(3)))
                .andExpect(jsonPath("$.distribucion[0].nivel").value("Bajo"))
                .andExpect(jsonPath("$.distribucion[0].total").value(2))
                .andExpect(jsonPath("$.distribucion[0].porcentaje").value(50.0))
                .andExpect(jsonPath("$.distribucion[1].nivel").value("Medio"))
                .andExpect(jsonPath("$.distribucion[1].total").value(1))
                .andExpect(jsonPath("$.distribucion[1].porcentaje").value(25.0))
                .andExpect(jsonPath("$.distribucion[2].nivel").value("Alto"))
                .andExpect(jsonPath("$.distribucion[2].total").value(1))
                .andExpect(jsonPath("$.distribucion[2].porcentaje").value(25.0));
    }

    @Test
    void solicitanteConVariasEvaluacionesSoloSeCuentaUnaVezSegunLaUltimaDelRango() throws Exception {
        Solicitante s1 = crearSolicitante("2001", "Ana", "Pérez");
        Solicitante s2 = crearSolicitante("2002", "Luis", "Gómez");

        // s1 tuvo Alto en octubre y Medio despues dentro de octubre; en noviembre paso a Bajo.
        guardarEvaluacion(s1, 200, NivelRiesgo.ALTO, LocalDateTime.of(2026, 10, 5, 9, 0));
        guardarEvaluacion(s1, 550, NivelRiesgo.MEDIO, LocalDateTime.of(2026, 10, 20, 15, 0));
        guardarEvaluacion(s1, 900, NivelRiesgo.BAJO, LocalDateTime.of(2026, 11, 2, 10, 0));

        // s2 tiene dos evaluaciones con exactamente la misma fecha en octubre -> desempata por id.
        LocalDateTime mismoInstante = LocalDateTime.of(2026, 10, 15, 12, 0, 0);
        guardarEvaluacion(s2, 250, NivelRiesgo.ALTO, mismoInstante);
        guardarEvaluacion(s2, 780, NivelRiesgo.BAJO, mismoInstante);

        // Filtrando octubre: s1 queda en Medio (su ultima en octubre) y s2 en Bajo.
        mockMvc.perform(get(ENDPOINT)
                        .param("fechaInicio", "2026-10-01")
                        .param("fechaFin", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSolicitantes").value(2))
                .andExpect(jsonPath("$.fechaInicio").value("2026-10-01"))
                .andExpect(jsonPath("$.fechaFin").value("2026-10-31"))
                .andExpect(jsonPath("$.distribucion[0].nivel").value("Bajo"))
                .andExpect(jsonPath("$.distribucion[0].total").value(1))
                .andExpect(jsonPath("$.distribucion[0].porcentaje").value(50.0))
                .andExpect(jsonPath("$.distribucion[1].nivel").value("Medio"))
                .andExpect(jsonPath("$.distribucion[1].total").value(1))
                .andExpect(jsonPath("$.distribucion[1].porcentaje").value(50.0))
                .andExpect(jsonPath("$.distribucion[2].nivel").value("Alto"))
                .andExpect(jsonPath("$.distribucion[2].total").value(0))
                .andExpect(jsonPath("$.distribucion[2].porcentaje").value(0.0));

        // Sin filtro de fechas: s1 toma la evaluacion mas reciente de noviembre (Bajo) -> 2 en Bajo.
        mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSolicitantes").value(2))
                .andExpect(jsonPath("$.distribucion[0].total").value(2))
                .andExpect(jsonPath("$.distribucion[0].porcentaje").value(100.0));
    }

    @Test
    void filtroIncluyeTodoElDiaDeFechaFin() throws Exception {
        Solicitante s1 = crearSolicitante("3001", "Ana", "Pérez");
        Solicitante s2 = crearSolicitante("3002", "Luis", "Gómez");

        guardarEvaluacion(s1, 800, NivelRiesgo.BAJO, LocalDateTime.of(2026, 10, 31, 23, 59, 59));
        guardarEvaluacion(s2, 200, NivelRiesgo.ALTO, LocalDateTime.of(2026, 11, 1, 0, 0, 0));

        mockMvc.perform(get(ENDPOINT)
                        .param("fechaInicio", "2026-10-31")
                        .param("fechaFin", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSolicitantes").value(1))
                .andExpect(jsonPath("$.distribucion[0].total").value(1));
    }

    @Test
    void periodoSinEvaluacionesDevuelveCerosYMensajeExacto() throws Exception {
        Solicitante s1 = crearSolicitante("4001", "Ana", "Pérez");
        guardarEvaluacion(s1, 800, NivelRiesgo.BAJO, LocalDateTime.of(2026, 9, 10, 10, 0));

        mockMvc.perform(get(ENDPOINT)
                        .param("fechaInicio", "2026-10-01")
                        .param("fechaFin", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSolicitantes").value(0))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_SIN_DATOS))
                .andExpect(jsonPath("$.distribucion", hasSize(3)))
                .andExpect(jsonPath("$.distribucion[0].nivel").value("Bajo"))
                .andExpect(jsonPath("$.distribucion[0].total").value(0))
                .andExpect(jsonPath("$.distribucion[0].porcentaje").value(0.0))
                .andExpect(jsonPath("$.distribucion[1].nivel").value("Medio"))
                .andExpect(jsonPath("$.distribucion[1].total").value(0))
                .andExpect(jsonPath("$.distribucion[1].porcentaje").value(0.0))
                .andExpect(jsonPath("$.distribucion[2].nivel").value("Alto"))
                .andExpect(jsonPath("$.distribucion[2].total").value(0))
                .andExpect(jsonPath("$.distribucion[2].porcentaje").value(0.0));
    }

    @Test
    void fechasInvalidasDevuelven400() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("fechaInicio", "2026-11-15")
                        .param("fechaFin", "2026-11-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Rango de fechas inválido"));

        mockMvc.perform(get(ENDPOINT)
                        .param("fechaInicio", "fecha-mala")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"));
    }

    @Test
    void seguridadExigeTokenYRolAdministrador() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isForbidden());
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
        // Se agregan dos variables para confirmar que el COUNT no multiplica filas.
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
