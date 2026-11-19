package com.udea.ScoreCrediticio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.udea.ScoreCrediticio.DAOs.FormulaScoreDAO;
import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO;
import com.udea.ScoreCrediticio.DAOs.RangoRiesgoDAO;
import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.DAOs.VariableRiesgoDAO;
import com.udea.ScoreCrediticio.Model.EstadoVariable;
import com.udea.ScoreCrediticio.Model.HistorialEvaluacion;
import com.udea.ScoreCrediticio.Model.NivelRiesgo;
import com.udea.ScoreCrediticio.Model.Solicitante;
import com.udea.ScoreCrediticio.Model.TipoDatoVariable;
import com.udea.ScoreCrediticio.Model.TipoDocumento;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

@SpringBootTest
@AutoConfigureMockMvc
class CalculoRiesgoIntegrationTest {

    // Escala continua de referencia: Bajo 701-1000, Medio 301-700, Alto 0-300.
    private static final String ESCALA_CONTINUA = "{"
            + "\"rangos\":["
            + "{\"nivel\":\"Bajo\",\"min\":701,\"max\":1000},"
            + "{\"nivel\":\"Medio\",\"min\":301,\"max\":700},"
            + "{\"nivel\":\"Alto\",\"min\":0,\"max\":300}]}";

    private static final String MENSAJE_DATOS_INCOMPLETOS =
            "No es posible calcular el riesgo crediticio: el solicitante no cuenta con la información completa requerida para la evaluación";
    private static final String MENSAJE_FORMULA_NO_VIGENTE =
            "El sistema no cuenta con una fórmula de scoring vigente configurada. Contacte al Administrador de Riesgos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitanteDAO solicitanteDAO;

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
        rangoRiesgoDAO.deleteAll();
        formulaScoreDAO.deleteAll();
        variableRiesgoDAO.deleteAll();
        solicitanteDAO.deleteAll();

        tokenAdmin = obtenerToken("admin@score.local", "Admin123*");
        tokenAnalista = obtenerToken("analista@score.local", "Analista123*");

        solicitante = solicitanteDAO.save(new Solicitante(null, "Ana", "Pérez", "3001234567",
                "ana@test.com", "12345678", TipoDocumento.CC));
    }

    // ---------- Criterio de aceptacion: calcular y guardar en el historial

    @Test
    void analistaCalculaRiesgoYGuardaElHistorial() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        // 80x40 + 60x35 + 90x25 = 7550; /10 = 755 puntos -> Bajo (701-1000).
        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitanteId").value(solicitante.getId()))
                .andExpect(jsonPath("$.puntaje").value(755))
                .andExpect(jsonPath("$.nivel").value("Bajo"))
                .andExpect(jsonPath("$.fechaCalculo").isNotEmpty())
                .andExpect(jsonPath("$.mensaje").value("Riesgo calculado: puntaje 755, nivel Bajo"));

        assertEquals(1, historialEvaluacionDAO.count());
        HistorialEvaluacion registro = historialEvaluacionDAO.findAll().get(0);
        assertEquals(755, registro.getPuntaje());
        assertEquals(NivelRiesgo.BAJO, registro.getNivel());
        assertEquals(solicitante.getId(), registro.getSolicitante().getId());
        assertNotNull(registro.getFechaCalculo());
    }

    @Test
    void calculosRepetidosSeAcumulanEnElHistorial() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isCreated());

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "100"), v(egresos, "100"), v(edad, "100"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntaje").value(1000));

        // El historial conserva ambos calculos, no sobreescribe.
        assertEquals(2, historialEvaluacionDAO.count());
    }

    @Test
    void calculoUsaLaFormulaVigenteDeCadaMomento() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "50.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "50.0000");
        guardarEscala();

        guardarFormula(ingresos, "50", egresos, "50");

        // 100x50 + 0x50 = 5000; /10 = 500 puntos -> Medio.
        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "100"), v(egresos, "0"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntaje").value(500))
                .andExpect(jsonPath("$.nivel").value("Medio"));

        // La nueva formula reemplaza a la anterior y el siguiente calculo la usa.
        guardarFormula(ingresos, "20", egresos, "80");

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "100"), v(egresos, "0"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntaje").value(200))
                .andExpect(jsonPath("$.nivel").value("Alto"));

        assertEquals(2, historialEvaluacionDAO.count());
        assertTrue(historialEvaluacionDAO.findAll().stream()
                .anyMatch(registro -> registro.getPuntaje() == 500));
        assertTrue(historialEvaluacionDAO.findAll().stream()
                .anyMatch(registro -> registro.getPuntaje() == 200));
    }

    @Test
    void puntajeCeroEsAltoYPuntajeMilEsBajo() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "0"), v(egresos, "0"), v(edad, "0"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntaje").value(0))
                .andExpect(jsonPath("$.nivel").value("Alto"));

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "100"), v(egresos, "100"), v(edad, "100"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntaje").value(1000))
                .andExpect(jsonPath("$.nivel").value("Bajo"));
    }

    @Test
    void redondeaElPuntajeHaciaArribaConDecimales() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        // 80.875x40 + 60x35 + 90x25 = 7585; /10 = 758.5 -> HALF_UP = 759.
        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80.875"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntaje").value(759));

        assertEquals(1, historialEvaluacionDAO.count());
        assertEquals(759, historialEvaluacionDAO.findAll().get(0).getPuntaje());
    }

    @Test
    void administradorTambienPuedeCalcularRiesgo() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        mockMvc.perform(calcular(tokenAdmin, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.puntaje").value(755));
    }

    // ---------- Errores de configuracion y de solicitud

    @Test
    void peticionSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/solicitantes/" + solicitante.getId() + "/calcular-riesgo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoValores("1:80")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solicitanteInexistenteDevuelve404() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        mockMvc.perform(calcular(tokenAnalista, 999999L,
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("999999")));

        assertEquals(0, historialEvaluacionDAO.count());
    }

    @Test
    void sinFormulaVigenteDevuelve404() throws Exception {
        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(), cuerpoValores("1:80")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value(MENSAJE_FORMULA_NO_VIGENTE))
                .andExpect(jsonPath("$.codigo").value("FORMULA_NO_VIGENTE"));

        assertEquals(0, historialEvaluacionDAO.count());
    }

    @Test
    void sinEscalaDeRiesgoConfiguradaDevuelve404() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "50.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "50.0000");

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ponderaciones\":["
                        + "{\"variableId\":%d,\"peso\":50},"
                        + "{\"variableId\":%d,\"peso\":50}]}",
                        ingresos.getId(), egresos.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("escala de niveles de riesgo")));

        assertEquals(0, historialEvaluacionDAO.count());
    }

    @Test
    void siLaFormulaVigenteQuedaIncompletaDevuelve404() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        // Inactivar una variable de la formula vigente la invalida (HU06/HU08).
        mockMvc.perform(patch("/api/variables/" + edad.getId() + "/estado")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value(MENSAJE_FORMULA_NO_VIGENTE))
                .andExpect(jsonPath("$.codigo").value("FORMULA_NO_VIGENTE"));

        assertEquals(0, historialEvaluacionDAO.count());
    }

    // ---------- HU11: datos obligatorios y variables faltantes

    @Test
    void datosObligatoriosFaltantesDevuelve400ConCamposFaltantes() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        Solicitante incompleto = solicitanteDAO.save(
                new Solicitante(null, null, null, null, null, null, null));

        mockMvc.perform(calcular(tokenAnalista, incompleto.getId(),
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Cálculo de riesgo inválido"))
                .andExpect(jsonPath("$.detail").value(MENSAJE_DATOS_INCOMPLETOS))
                .andExpect(jsonPath("$.codigo").value("DATOS_INCOMPLETOS"))
                .andExpect(jsonPath("$.camposFaltantes", org.hamcrest.Matchers.contains(
                        "tipoDocumento", "numeroDocumento", "nombre", "apellido", "telefono", "email")))
                .andExpect(jsonPath("$.variablesFaltantes").isEmpty());

        assertEquals(0, historialEvaluacionDAO.count());
    }

    @Test
    void variablesFaltantesSeInformanEnElJson() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Cálculo de riesgo inválido"))
                .andExpect(jsonPath("$.detail").value(MENSAJE_DATOS_INCOMPLETOS))
                .andExpect(jsonPath("$.codigo").value("DATOS_INCOMPLETOS"))
                .andExpect(jsonPath("$.camposFaltantes").isEmpty())
                .andExpect(jsonPath("$.variablesFaltantes", org.hamcrest.Matchers.contains(
                        "Egresos", "Edad")));

        assertEquals(0, historialEvaluacionDAO.count());
    }

    @Test
    void valoresVaciosONulosCuentanComoVariablesFaltantes() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        // Lista vacia: faltan todas las variables de la formula.
        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(), "{\"valores\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MENSAJE_DATOS_INCOMPLETOS))
                .andExpect(jsonPath("$.variablesFaltantes", org.hamcrest.Matchers.contains(
                        "Ingresos", "Egresos", "Edad")));

        // Valor null explicito: solo esa variable se reporta como faltante.
        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                "{\"valores\":["
                        + "{\"variableId\":" + ingresos.getId() + ",\"valor\":null},"
                        + "{\"variableId\":" + egresos.getId() + ",\"valor\":60},"
                        + "{\"variableId\":" + edad.getId() + ",\"valor\":90}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MENSAJE_DATOS_INCOMPLETOS))
                .andExpect(jsonPath("$.variablesFaltantes", org.hamcrest.Matchers.contains("Ingresos")));

        // Sin el campo valores tambien se reportan todas las variables.
        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.variablesFaltantes", org.hamcrest.Matchers.contains(
                        "Ingresos", "Egresos", "Edad")));

        assertEquals(0, historialEvaluacionDAO.count());
    }

    @Test
    void valoresDeVariablesAjenasODuplicadasDevuelve400() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"), v(egresos, "60"), v(edad, "90"), "999999:50")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail",
                        org.hamcrest.Matchers.containsString("no pertenecen a la fórmula vigente")));

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "80"), v(ingresos, "70"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("más de una vez")));

        assertEquals(0, historialEvaluacionDAO.count());
    }

    @Test
    void valoresFueraDeRangoDevuelven400() throws Exception {
        VariableRiesgo ingresos = crearVariable("Ingresos", "40.0000");
        VariableRiesgo egresos = crearVariable("Egresos", "35.0000");
        VariableRiesgo edad = crearVariable("Edad", "25.0000");
        guardarFormulaYEscala(ingresos, egresos, edad);

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "101"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"));

        mockMvc.perform(calcular(tokenAnalista, solicitante.getId(),
                cuerpoValores(v(ingresos, "-1"), v(egresos, "60"), v(edad, "90"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"));

        assertEquals(0, historialEvaluacionDAO.count());
    }

    // ---------- Helpers

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

        guardarEscala();
    }

    private void guardarFormula(VariableRiesgo primera, String pesoPrimera,
            VariableRiesgo segunda, String pesoSegunda) throws Exception {
        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ponderaciones\":["
                        + "{\"variableId\":%d,\"peso\":%s},"
                        + "{\"variableId\":%d,\"peso\":%s}]}",
                        primera.getId(), pesoPrimera, segunda.getId(), pesoSegunda)))
                .andExpect(status().isCreated());
    }

    private void guardarEscala() throws Exception {
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
