package com.udea.ScoreCrediticio;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

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
import com.udea.ScoreCrediticio.DAOs.RangoRiesgoDAO;
import com.udea.ScoreCrediticio.DAOs.VariableRiesgoDAO;
import com.udea.ScoreCrediticio.Model.EstadoVariable;
import com.udea.ScoreCrediticio.Model.TipoDatoVariable;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

@SpringBootTest
@AutoConfigureMockMvc
class AuditoriaIntegrationTest {

    private static final String ESCALA_INICIAL = "{"
            + "\"rangos\":["
            + "{\"nivel\":\"Bajo\",\"min\":701,\"max\":1000},"
            + "{\"nivel\":\"Medio\",\"min\":301,\"max\":700},"
            + "{\"nivel\":\"Alto\",\"min\":0,\"max\":300}]}";

    private static final String ESCALA_NUEVA = "{"
            + "\"rangos\":["
            + "{\"nivel\":\"Bajo\",\"min\":501,\"max\":1000},"
            + "{\"nivel\":\"Medio\",\"min\":201,\"max\":500},"
            + "{\"nivel\":\"Alto\",\"min\":0,\"max\":200}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VariableRiesgoDAO variableRiesgoDAO;

    @Autowired
    private FormulaScoreDAO formulaScoreDAO;

    @Autowired
    private RangoRiesgoDAO rangoRiesgoDAO;

    private String tokenAdmin;
    private String tokenAnalista;

    @BeforeEach
    void preparar() throws Exception {
        formulaScoreDAO.deleteAll();
        variableRiesgoDAO.deleteAll();
        rangoRiesgoDAO.deleteAll();
        jdbcTemplate.update("DELETE FROM bitacora_auditoria");

        tokenAdmin = obtenerToken("admin@score.local", "Admin123*");
        tokenAnalista = obtenerToken("analista@score.local", "Analista123*");
    }

    // ---------- Escenario 1: registro automatico de cambios

    @Test
    void altaDeVariableGeneraRegistroConUsuarioYValores() throws Exception {
        mockMvc.perform(crearVariable(tokenAdmin, "Ingresos", "40"))
                .andExpect(status().isCreated());

        MvcResult resultado = mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].usuario").value("admin@score.local"))
                .andExpect(jsonPath("$[0].modulo").value("VARIABLE_RIESGO"))
                .andExpect(jsonPath("$[0].accion").value("CREAR"))
                .andExpect(jsonPath("$[0].fechaUtc").isNotEmpty())
                .andExpect(jsonPath("$[0].valorAnterior").value(nullValue()))
                .andExpect(jsonPath("$[0].valorNuevo.nombre").value("Ingresos"))
                .andExpect(jsonPath("$[0].valorNuevo.tipoDato").value("NUMERICO"))
                .andExpect(jsonPath("$[0].valorNuevo.peso").value(40.0))
                .andExpect(jsonPath("$[0].valorNuevo.estado").value("Activa"))
                .andReturn();

        String fechaUtc = JsonPath.read(resultado.getResponse().getContentAsString(), "$[0].fechaUtc");
        assertEquals(ZoneOffset.UTC, OffsetDateTime.parse(fechaUtc).getOffset());

        Object valorNuevo = JsonPath.read(resultado.getResponse().getContentAsString(),
                "$[0].valorNuevo");
        assertTrue(valorNuevo instanceof Map, "valorNuevo debe ser un objeto JSON");
    }

    @Test
    void cambioDeEstadoRegistraValoresAnteriorYNuevo() throws Exception {
        Long id = crearVariableYDevolverId(tokenAdmin, "Ingresos", "60");

        mockMvc.perform(patch("/api/variables/{id}/estado", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].accion").value("ACTUALIZAR"))
                .andExpect(jsonPath("$[0].valorAnterior.estado").value("Activa"))
                .andExpect(jsonPath("$[0].valorNuevo.estado").value("Inactiva"));
    }

    @Test
    void guardarFormulaAuditaCreacionYReemplazo() throws Exception {
        VariableRiesgo ingresos = crearVariableDirecta("Ingresos", TipoDatoVariable.NUMERICO, "40");
        VariableRiesgo egresos = crearVariableDirecta("Egresos", TipoDatoVariable.NUMERICO, "35");
        VariableRiesgo edad = crearVariableDirecta("Edad", TipoDatoVariable.NUMERICO, "25");
        jdbcTemplate.update("DELETE FROM bitacora_auditoria");

        String primeraFormula = formulaBody(
                new long[] { ingresos.getId(), egresos.getId(), edad.getId() },
                new int[] { 40, 35, 25 });

        mockMvc.perform(post("/api/formula")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(primeraFormula))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].modulo").value("FORMULA_SCORE"))
                .andExpect(jsonPath("$[0].accion").value("CREAR"))
                .andExpect(jsonPath("$[0].valorAnterior").value(nullValue()))
                .andExpect(jsonPath("$[0].valorNuevo.vigente").value(true))
                .andExpect(jsonPath("$[0].valorNuevo.sumaTotal").value(100.0))
                .andExpect(jsonPath("$[0].valorNuevo.ponderaciones", hasSize(3)));

        String segundaFormula = formulaBody(
                new long[] { ingresos.getId(), egresos.getId(), edad.getId() },
                new int[] { 50, 30, 20 });

        mockMvc.perform(post("/api/formula")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(segundaFormula))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].accion").value("CREAR"))
                .andExpect(jsonPath("$[0].valorNuevo.vigente").value(true))
                .andExpect(jsonPath("$[0].valorNuevo.ponderaciones", hasSize(3)))
                .andExpect(jsonPath("$[1].accion").value("ACTUALIZAR"))
                .andExpect(jsonPath("$[1].valorAnterior.vigente").value(true))
                .andExpect(jsonPath("$[1].valorNuevo.vigente").value(false))
                .andExpect(jsonPath("$[2].accion").value("CREAR"));
    }

    @Test
    void guardarRangosAuditaValoresAnterioresYNuevosPorNivel() throws Exception {
        mockMvc.perform(guardarRangos(tokenAdmin, ESCALA_INICIAL))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].modulo").value("RANGO_RIESGO"))
                .andExpect(jsonPath("$[0].accion").value("CREAR"));

        jdbcTemplate.update("DELETE FROM bitacora_auditoria");

        mockMvc.perform(guardarRangos(tokenAdmin, ESCALA_NUEVA))
                .andExpect(status().isCreated());

        MvcResult resultado = mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].accion").value("ACTUALIZAR"))
                .andReturn();

        List<Map<String, Object>> registros = JsonPath.read(
                resultado.getResponse().getContentAsString(), "$");

        Map<String, Object> registroBajo = registros.stream()
                .filter(registro -> "Bajo".equals(valorDe(registro, "valorNuevo", "nivel")))
                .findFirst()
                .orElseThrow();

        assertEquals(701, numeroDe(registroBajo, "valorAnterior", "puntajeMinimo"));
        assertEquals(501, numeroDe(registroBajo, "valorNuevo", "puntajeMinimo"));
        assertEquals(1000, numeroDe(registroBajo, "valorAnterior", "puntajeMaximo"));
        assertEquals(1000, numeroDe(registroBajo, "valorNuevo", "puntajeMaximo"));
        assertEquals(3, rangoRiesgoDAO.count());
    }

    @Test
    void operacionFallidaNoGeneraRegistroDeAuditoria() throws Exception {
        mockMvc.perform(crearVariable(tokenAdmin, "Ingresos", "60"))
                .andExpect(status().isCreated());

        mockMvc.perform(crearVariable(tokenAdmin, "Egresos", "50"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void consultarNoGeneraAuditoriaYElCambioIdempotenteNoDuplica() throws Exception {
        Long id = crearVariableYDevolverId(tokenAdmin, "Ingresos", "60");

        mockMvc.perform(get("/api/variables")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/variables/{id}/estado", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/variables/{id}/estado", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ---------- Escenario 2: consulta de la bitacora

    @Test
    void listadoSeOrdenaDelMasRecienteAlMasAntiguo() throws Exception {
        mockMvc.perform(crearVariable(tokenAdmin, "Primera", "10"))
                .andExpect(status().isCreated());
        mockMvc.perform(crearVariable(tokenAdmin, "Segunda", "20"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].valorNuevo.nombre").value("Segunda"))
                .andExpect(jsonPath("$[1].valorNuevo.nombre").value("Primera"));
    }

    @Test
    void soloAdministradorPuedeConsultarLaBitacora() throws Exception {
        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auditoria"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laBitacoraNoTieneEndpointsDeEscritura() throws Exception {
        mockMvc.perform(put("/api/auditoria/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(delete("/api/auditoria/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().is4xxClientError());
    }

    // ---------- Helpers

    private MockHttpServletRequestBuilder crearVariable(String token, String nombre, String peso) {
        String cuerpo = "{\"nombre\":\"" + nombre + "\",\"tipoDato\":\"NUMERICO\""
                + (peso == null ? "" : ",\"peso\":" + peso) + "}";

        return post("/api/variables")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    private Long crearVariableYDevolverId(String token, String nombre, String peso) throws Exception {
        MvcResult resultado = mockMvc.perform(crearVariable(token, nombre, peso))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private VariableRiesgo crearVariableDirecta(String nombre, TipoDatoVariable tipoDato, String peso) {
        VariableRiesgo variable = new VariableRiesgo(nombre, tipoDato, new BigDecimal(peso),
                EstadoVariable.ACTIVA);
        return variableRiesgoDAO.saveAndFlush(variable);
    }

    private String formulaBody(long[] ids, int[] pesos) {
        StringBuilder body = new StringBuilder("{\"ponderaciones\":[");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                body.append(',');
            }
            body.append("{\"variableId\":").append(ids[i])
                    .append(",\"peso\":").append(pesos[i]).append('}');
        }
        return body.append("]}").toString();
    }

    private MockHttpServletRequestBuilder guardarRangos(String token, String cuerpo) {
        return post("/api/rangos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    private Object valorDe(Map<String, Object> registro, String campo, String propiedad) {
        Object valor = registro.get(campo);
        assertNotNull(valor, campo + " no debe ser nulo");
        return ((Map<?, ?>) valor).get(propiedad);
    }

    private int numeroDe(Map<String, Object> registro, String campo, String propiedad) {
        return ((Number) valorDe(registro, campo, propiedad)).intValue();
    }

    private String obtenerToken(String email, String password) throws Exception {
        MockHttpServletRequestBuilder login = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        MvcResult result = mockMvc.perform(login).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
