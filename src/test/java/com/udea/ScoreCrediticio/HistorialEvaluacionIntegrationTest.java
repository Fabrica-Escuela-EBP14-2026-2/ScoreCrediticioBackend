package com.udea.ScoreCrediticio;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
import com.udea.ScoreCrediticio.DAOs.FormulaScoreDAO;
import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO;
import com.udea.ScoreCrediticio.DAOs.PerfilFinancieroDAO;
import com.udea.ScoreCrediticio.DAOs.RangoRiesgoDAO;
import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.DAOs.VariableRiesgoDAO;
import com.udea.ScoreCrediticio.Model.EstadoVariable;
import com.udea.ScoreCrediticio.Model.HistorialEvaluacion;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;
import com.udea.ScoreCrediticio.Model.PerfilFinanciero;
import com.udea.ScoreCrediticio.Model.Solicitante;
import com.udea.ScoreCrediticio.Model.TipoDatoVariable;
import com.udea.ScoreCrediticio.Model.TipoDocumento;
import com.udea.ScoreCrediticio.Model.VariableEvaluada;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

@SpringBootTest
@AutoConfigureMockMvc
class HistorialEvaluacionIntegrationTest {

    // Escala continua de referencia: Bajo 701-1000, Medio 301-700, Alto 0-300.
    private static final String ESCALA_CONTINUA = "{"
            + "\"rangos\":["
            + "{\"nivel\":\"Bajo\",\"min\":701,\"max\":1000},"
            + "{\"nivel\":\"Medio\",\"min\":301,\"max\":700},"
            + "{\"nivel\":\"Alto\",\"min\":0,\"max\":300}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SolicitanteDAO solicitanteDAO;

    @Autowired
    private PerfilFinancieroDAO perfilFinancieroDAO;

    @Autowired
    private VariableRiesgoDAO variableRiesgoDAO;

    @Autowired
    private FormulaScoreDAO formulaScoreDAO;

    @Autowired
    private RangoRiesgoDAO rangoRiesgoDAO;

    @Autowired
    private HistorialEvaluacionDAO historialEvaluacionDAO;

    private String tokenAdmin;
    private String tokenAnalista;
    private Solicitante solicitante;

    @BeforeEach
    void preparar() throws Exception {
        historialEvaluacionDAO.deleteAll();
        perfilFinancieroDAO.deleteAll();
        rangoRiesgoDAO.deleteAll();
        formulaScoreDAO.deleteAll();
        variableRiesgoDAO.deleteAll();
        solicitanteDAO.deleteAll();

        tokenAdmin = obtenerToken("admin@score.local", "Admin123*");
        tokenAnalista = obtenerToken("analista@score.local", "Analista123*");

        solicitante = solicitanteDAO.save(new Solicitante(null, "Ana", "Pérez", "3001234567",
                "ana@test.com", "12345678", TipoDocumento.CC));
    }

    // ---------- Criterio de aceptacion: historial y detalle

    @Test
    void solicitanteSinEvaluacionesDevuelveListaVacia() throws Exception {
        mockMvc.perform(get("/api/solicitantes/" + solicitante.getId() + "/historial")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void historialDevuelveDetalleFinancieroYVariablesDelCalculo() throws Exception {
        crearPerfil("2500000.00", "1200000.00");

        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntaje").value(755));

        mockMvc.perform(get("/api/solicitantes/" + solicitante.getId() + "/historial")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].solicitanteId").value(solicitante.getId()))
                .andExpect(jsonPath("$[0].puntaje").value(755))
                .andExpect(jsonPath("$[0].nivel").value("Bajo"))
                .andExpect(jsonPath("$[0].fechaCalculo").isNotEmpty())
                .andExpect(jsonPath("$[0].evaluadorEmail").value("analista@score.local"))
                .andExpect(jsonPath("$[0].detalle.ingresos").value(2500000.0))
                .andExpect(jsonPath("$[0].detalle.egresos").value(1200000.0))
                .andExpect(jsonPath("$[0].detalle.ingresoNetoDisponible").value(1300000.0))
                .andExpect(jsonPath("$[0].detalle.variables", hasSize(3)))
                .andExpect(jsonPath("$[0].detalle.variables[*].nombre",
                        containsInAnyOrder("Ingresos", "Egresos", "Edad")))
                .andExpect(jsonPath("$[0].detalle.variables[?(@.nombre == 'Ingresos')].valor",
                        contains(80.0)))
                .andExpect(jsonPath("$[0].detalle.variables[?(@.nombre == 'Ingresos')].peso",
                        contains(40.0)))
                .andExpect(jsonPath("$[0].detalle.variables[?(@.nombre == 'Egresos')].valor",
                        contains(60.0)))
                .andExpect(jsonPath("$[0].detalle.variables[?(@.nombre == 'Edad')].valor",
                        contains(90.0)));
    }

    @Test
    void historialOrdenaDeLaEvaluacionMasRecienteALaMasAntigua() throws Exception {
        HistorialEvaluacion antigua = new HistorialEvaluacion(solicitante, 400, NivelRiesgo.MEDIO,
                "analista@score.local", null, null);
        antigua.agregarVariable(new VariableEvaluada(
                1L, "Ingresos", new BigDecimal("50.0000"), new BigDecimal("40.0000")));

        HistorialEvaluacion reciente = new HistorialEvaluacion(solicitante, 800, NivelRiesgo.BAJO,
                "analista@score.local", null, null);
        reciente.agregarVariable(new VariableEvaluada(
                1L, "Ingresos", new BigDecimal("90.0000"), new BigDecimal("40.0000")));

        antigua = historialEvaluacionDAO.saveAndFlush(antigua);
        reciente = historialEvaluacionDAO.saveAndFlush(reciente);

        jdbcTemplate.update("UPDATE historial_evaluacion SET fecha_calculo = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(2)), antigua.getId());
        jdbcTemplate.update("UPDATE historial_evaluacion SET fecha_calculo = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now()), reciente.getId());

        mockMvc.perform(get("/api/solicitantes/" + solicitante.getId() + "/historial")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(reciente.getId()))
                .andExpect(jsonPath("$[0].detalle.variables", hasSize(1)))
                .andExpect(jsonPath("$[0].detalle.variables[0].valor").value(90.0))
                .andExpect(jsonPath("$[1].id").value(antigua.getId()))
                .andExpect(jsonPath("$[1].detalle.variables", hasSize(1)))
                .andExpect(jsonPath("$[1].detalle.variables[0].valor").value(50.0));
    }

    @Test
    void elDetalleConservaElSnapshotAunqueCambienLosDatosActuales() throws Exception {
        crearPerfil("2500000.00", "1200000.00");

        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isCreated());

        // Se modifican los datos actuales despues del calculo.
        PerfilFinanciero perfil = perfilFinancieroDAO.findBySolicitanteId(solicitante.getId())
                .orElseThrow();
        perfil.setIngresos(new BigDecimal("9000000.00"));
        perfil.setEgresos(new BigDecimal("1000000.00"));
        perfilFinancieroDAO.saveAndFlush(perfil);

        ingresos.setNombre("Ingresos actuales");
        ingresos.setPeso(new BigDecimal("99.0000"));
        variableRiesgoDAO.saveAndFlush(ingresos);

        mockMvc.perform(get("/api/solicitantes/" + solicitante.getId() + "/historial")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].detalle.ingresos").value(2500000.0))
                .andExpect(jsonPath("$[0].detalle.egresos").value(1200000.0))
                .andExpect(jsonPath("$[0].detalle.ingresoNetoDisponible").value(1300000.0))
                .andExpect(jsonPath("$[0].detalle.variables[?(@.nombre == 'Ingresos')].valor",
                        contains(80.0)))
                .andExpect(jsonPath("$[0].detalle.variables[?(@.nombre == 'Ingresos')].peso",
                        contains(40.0)));
    }

    @Test
    void sinPerfilFinancieroElDetalleDevuelveDatosNulos() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/solicitantes/" + solicitante.getId() + "/historial")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].evaluadorEmail").value("analista@score.local"))
                .andExpect(jsonPath("$[0].detalle.ingresos").value(nullValue()))
                .andExpect(jsonPath("$[0].detalle.egresos").value(nullValue()))
                .andExpect(jsonPath("$[0].detalle.ingresoNetoDisponible").value(nullValue()))
                .andExpect(jsonPath("$[0].detalle.variables", hasSize(3)));
    }

    @Test
    void cadaSolicitanteSoloVeSusPropiasEvaluaciones() throws Exception {
        Solicitante otro = solicitanteDAO.save(new Solicitante(null, "Luis", "Gómez", "3007654321",
                "luis@test.com", "87654321", TipoDocumento.CC));

        historialEvaluacionDAO.saveAndFlush(new HistorialEvaluacion(
                solicitante, 500, NivelRiesgo.MEDIO, "analista@score.local", null, null));
        historialEvaluacionDAO.saveAndFlush(new HistorialEvaluacion(
                otro, 900, NivelRiesgo.BAJO, "analista@score.local", null, null));

        mockMvc.perform(get("/api/solicitantes/" + solicitante.getId() + "/historial")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].solicitanteId").value(solicitante.getId()))
                .andExpect(jsonPath("$[0].puntaje").value(500));
    }

    @Test
    void evaluacionSinSnapshotDevuelveDetalleVacioYEvaluadorNulo() throws Exception {
        historialEvaluacionDAO.saveAndFlush(new HistorialEvaluacion(
                solicitante, 500, NivelRiesgo.MEDIO));

        mockMvc.perform(get("/api/solicitantes/" + solicitante.getId() + "/historial")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].evaluadorEmail").value(nullValue()))
                .andExpect(jsonPath("$[0].detalle.ingresos").value(nullValue()))
                .andExpect(jsonPath("$[0].detalle.egresos").value(nullValue()))
                .andExpect(jsonPath("$[0].detalle.variables", hasSize(0)));
    }

    // ---------- Errores del endpoint

    @Test
    void peticionSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/solicitantes/" + solicitante.getId() + "/historial"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solicitanteInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/solicitantes/999999/historial")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail", containsString("999999")));
    }

    // ---------- Helpers

    private void crearPerfil(String ingresos, String egresos) {
        perfilFinancieroDAO.save(new PerfilFinanciero(
                null, new BigDecimal(ingresos), new BigDecimal(egresos), solicitante));
    }

    private VariableRiesgo crearVariable(String nombre, String peso) {
        return variableRiesgoDAO.save(new VariableRiesgo(nombre, TipoDatoVariable.NUMERICO,
                new BigDecimal(peso), EstadoVariable.ACTIVA));
    }

    private void guardarFormulaYEscala(VariableRiesgo ingresos, VariableRiesgo egresos, VariableRiesgo edad)
            throws Exception {
        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ponderaciones\":["
                        + "{\"variableId\":%d,\"peso\":40},"
                        + "{\"variableId\":%d,\"peso\":35},"
                        + "{\"variableId\":%d,\"peso\":25}]}",
                        ingresos.getId(), egresos.getId(), edad.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/rangos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ESCALA_CONTINUA))
                .andExpect(status().isCreated());
    }

    private MockHttpServletRequestBuilder calcular(String token, Long solicitanteId, String cuerpo) {
        MockHttpServletRequestBuilder peticion = post("/api/solicitantes/" + solicitanteId + "/calcular-riesgo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);

        return token == null ? peticion : peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String v(VariableRiesgo variable, String valor) {
        return variable.getId() + ":" + valor;
    }

    private String cuerpoValores(String... paresVariableValor) {
        StringBuilder cuerpo = new StringBuilder("{\"valores\":[");
        for (int i = 0; i < paresVariableValor.length; i++) {
            String[] partes = paresVariableValor[i].split(":");
            if (i > 0) {
                cuerpo.append(",");
            }
            cuerpo.append("{\"variableId\":").append(partes[0])
                    .append(",\"valor\":").append(partes[1]).append("}");
        }
        return cuerpo.append("]}").toString();
    }

    private String obtenerToken(String email, String password) throws Exception {
        MockHttpServletRequestBuilder login = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        MvcResult result = mockMvc.perform(login).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
