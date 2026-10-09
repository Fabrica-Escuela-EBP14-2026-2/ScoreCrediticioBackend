package com.udea.ScoreCrediticio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

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
import com.udea.ScoreCrediticio.DAOs.VariableRiesgoDAO;
import com.udea.ScoreCrediticio.Model.EstadoVariable;
import com.udea.ScoreCrediticio.Model.FormulaScore;
import com.udea.ScoreCrediticio.Model.TipoDatoVariable;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

@SpringBootTest
@AutoConfigureMockMvc
class FormulaScoreIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FormulaScoreDAO formulaScoreDAO;

    @Autowired
    private VariableRiesgoDAO variableRiesgoDAO;

    private String tokenAdmin;
    private String tokenAnalista;

    @BeforeEach
    void preparar() throws Exception {
        formulaScoreDAO.deleteAll();
        variableRiesgoDAO.deleteAll();
        tokenAdmin = obtenerToken("admin@score.local", "Admin123*");
        tokenAnalista = obtenerToken("analista@score.local", "Analista123*");
    }

    // ---------- Criterio de Aceptacion 1: Guardado exitoso y formula vigente

    @Test
    void administradorGuardaFormulaConSumaExacta100YQuedaVigente() throws Exception {
        VariableRiesgo var1 = crearVariable("Ingresos", TipoDatoVariable.NUMERICO, "40.0000", EstadoVariable.ACTIVA);
        VariableRiesgo var2 = crearVariable("Egresos", TipoDatoVariable.NUMERICO, "35.0000", EstadoVariable.ACTIVA);
        VariableRiesgo var3 = crearVariable("Edad", TipoDatoVariable.NUMERICO, "25.0000", EstadoVariable.ACTIVA);

        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":40},"
                + "{\"variableId\":%d,\"peso\":35},"
                + "{\"variableId\":%d,\"peso\":25}]}",
                var1.getId(), var2.getId(), var3.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.mensaje").value("Fórmula de scoring guardada e implementada con éxito"))
                .andExpect(jsonPath("$.estado").value("Vigente"))
                .andExpect(jsonPath("$.vigente").value(true))
                .andExpect(jsonPath("$.sumaTotal").value(100.0000))
                .andExpect(jsonPath("$.fechaImplementacion").isNotEmpty())
                .andExpect(jsonPath("$.ponderaciones.length()").value(3))
                .andExpect(jsonPath("$.ponderaciones[0].nombreVariable").value("Ingresos"))
                .andExpect(jsonPath("$.ponderaciones[0].peso").value(40.0000));

        // Verificacion en base de datos
        assertTrue(formulaScoreDAO.findByVigenteTrue().isPresent());
        FormulaScore guardada = formulaScoreDAO.findByVigenteTrue().get();
        assertEquals("VIGENTE", guardada.getClaveVigencia());
        assertTrue(guardada.isVigente());
    }

    @Test
    void consultarFormulaVigenteDevuelveLaFormulaActiva() throws Exception {
        VariableRiesgo var1 = crearVariable("Ingresos", TipoDatoVariable.NUMERICO, "60.0000", EstadoVariable.ACTIVA);
        VariableRiesgo var2 = crearVariable("Historial", TipoDatoVariable.CATEGORICO, "40.0000", EstadoVariable.ACTIVA);

        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":60},"
                + "{\"variableId\":%d,\"peso\":40}]}",
                var1.getId(), var2.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/formula/vigente")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Fórmula de scoring vigente"))
                .andExpect(jsonPath("$.estado").value("Vigente"))
                .andExpect(jsonPath("$.vigente").value(true))
                .andExpect(jsonPath("$.sumaTotal").value(100.0000))
                .andExpect(jsonPath("$.ponderaciones.length()").value(2));
    }

    @Test
    void guardarNuevaFormulaDesactivaLaFormulaVigenteAnterior() throws Exception {
        VariableRiesgo var1 = crearVariable("Ingresos", TipoDatoVariable.NUMERICO, "50.0000", EstadoVariable.ACTIVA);
        VariableRiesgo var2 = crearVariable("Egresos", TipoDatoVariable.NUMERICO, "50.0000", EstadoVariable.ACTIVA);

        // Primera formula (50% y 50%)
        String cuerpo1 = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":50},"
                + "{\"variableId\":%d,\"peso\":50}]}",
                var1.getId(), var2.getId());

        MvcResult res1 = mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo1))
                .andExpect(status().isCreated())
                .andReturn();

        Long id1 = ((Number) JsonPath.read(res1.getResponse().getContentAsString(), "$.id")).longValue();

        // Segunda formula con ponderaciones diferentes (70% y 30%)
        String cuerpo2 = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":70},"
                + "{\"variableId\":%d,\"peso\":30}]}",
                var1.getId(), var2.getId());

        MvcResult res2 = mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo2))
                .andExpect(status().isCreated())
                .andReturn();

        Long id2 = ((Number) JsonPath.read(res2.getResponse().getContentAsString(), "$.id")).longValue();

        // La formula anterior ahora esta inactiva y no tiene clave_vigencia
        FormulaScore formulaAnterior = formulaScoreDAO.findById(id1).orElseThrow();
        assertFalse(formulaAnterior.isVigente());
        assertEquals(null, formulaAnterior.getClaveVigencia());

        // La formula nueva es la unica vigente
        FormulaScore formulaNueva = formulaScoreDAO.findById(id2).orElseThrow();
        assertTrue(formulaNueva.isVigente());
        assertEquals("VIGENTE", formulaNueva.getClaveVigencia());

        // El endpoint GET devuelve la nueva
        mockMvc.perform(get("/api/formula/vigente")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id2))
                .andExpect(jsonPath("$.ponderaciones[0].peso").value(70.0000));
    }

    @Test
    void consultarFormulaVigenteSinFormulasDevuelve404() throws Exception {
        mockMvc.perform(get("/api/formula/vigente")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    // ---------- Criterio de Aceptacion 2: Suma inferior o superior al 100%

    @Test
    void sumaInferiorA100RechazaConDetalleDeFaltanteYNoGuarda() throws Exception {
        VariableRiesgo var1 = crearVariable("Ingresos", TipoDatoVariable.NUMERICO, "50.0000", EstadoVariable.ACTIVA);
        VariableRiesgo var2 = crearVariable("Egresos", TipoDatoVariable.NUMERICO, "40.0000", EstadoVariable.ACTIVA);

        // Suma 50 + 40 = 90%
        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":50},"
                + "{\"variableId\":%d,\"peso\":40}]}",
                var1.getId(), var2.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Suma de pesos de la fórmula inválida"))
                .andExpect(jsonPath("$.totalCalculado").value("90%"))
                .andExpect(jsonPath("$.diferencia").value("10%"))
                .andExpect(jsonPath("$.tipoDescuadre").value("FALTANTE"));

        // No se guardo ninguna formula
        assertTrue(formulaScoreDAO.findAll().isEmpty());
    }

    @Test
    void sumaSuperiorA100RechazaConDetalleDeExcedenteYNoGuarda() throws Exception {
        VariableRiesgo var1 = crearVariable("Ingresos", TipoDatoVariable.NUMERICO, "60.0000", EstadoVariable.ACTIVA);
        VariableRiesgo var2 = crearVariable("Egresos", TipoDatoVariable.NUMERICO, "55.0000", EstadoVariable.ACTIVA);

        // Suma 60 + 55 = 115%
        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":60},"
                + "{\"variableId\":%d,\"peso\":55}]}",
                var1.getId(), var2.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Suma de pesos de la fórmula inválida"))
                .andExpect(jsonPath("$.totalCalculado").value("115%"))
                .andExpect(jsonPath("$.diferencia").value("15%"))
                .andExpect(jsonPath("$.tipoDescuadre").value("EXCEDENTE"));

        assertTrue(formulaScoreDAO.findAll().isEmpty());
    }

    @Test
    void sumaExactaConCuatroDecimalesSeAcepta() throws Exception {
        VariableRiesgo var1 = crearVariable("Var1", TipoDatoVariable.NUMERICO, "33.3333", EstadoVariable.ACTIVA);
        VariableRiesgo var2 = crearVariable("Var2", TipoDatoVariable.NUMERICO, "33.3333", EstadoVariable.ACTIVA);
        VariableRiesgo var3 = crearVariable("Var3", TipoDatoVariable.NUMERICO, "33.3334", EstadoVariable.ACTIVA);

        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":33.3333},"
                + "{\"variableId\":%d,\"peso\":33.3333},"
                + "{\"variableId\":%d,\"peso\":33.3334}]}",
                var1.getId(), var2.getId(), var3.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sumaTotal").value(100.0000));
    }

    // ---------- Criterio de Aceptacion 3: Exclusion de variables inactivas y cobertura

    @Test
    void rechazarSiSeIncluyeVariableInactiva() throws Exception {
        VariableRiesgo activa = crearVariable("Activa", TipoDatoVariable.NUMERICO, "70.0000", EstadoVariable.ACTIVA);
        VariableRiesgo inactiva = crearVariable("Inactiva", TipoDatoVariable.NUMERICO, "30.0000", EstadoVariable.INACTIVA);

        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":70},"
                + "{\"variableId\":%d,\"peso\":30}]}",
                activa.getId(), inactiva.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Fórmula de scoring inválida"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("está inactiva")));

        assertTrue(formulaScoreDAO.findAll().isEmpty());
    }

    @Test
    void excluirVariablesInactivasDeLaFormulaYPermitirPonderarSoloActivas() throws Exception {
        VariableRiesgo activa1 = crearVariable("Activa1", TipoDatoVariable.NUMERICO, "60.0000", EstadoVariable.ACTIVA);
        VariableRiesgo activa2 = crearVariable("Activa2", TipoDatoVariable.PORCENTAJE, "40.0000", EstadoVariable.ACTIVA);
        // Variable inactiva en el catalogo
        crearVariable("Historica", TipoDatoVariable.CATEGORICO, "50.0000", EstadoVariable.INACTIVA);

        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":60},"
                + "{\"variableId\":%d,\"peso\":40}]}",
                activa1.getId(), activa2.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ponderaciones.length()").value(2));
    }

    @Test
    void rechazarSiNoSePonderanTodasLasVariablesActivas() throws Exception {
        VariableRiesgo var1 = crearVariable("Var1", TipoDatoVariable.NUMERICO, "40.0000", EstadoVariable.ACTIVA);
        VariableRiesgo var2 = crearVariable("Var2", TipoDatoVariable.NUMERICO, "30.0000", EstadoVariable.ACTIVA);
        VariableRiesgo var3 = crearVariable("Var3", TipoDatoVariable.NUMERICO, "30.0000", EstadoVariable.ACTIVA);

        // Se envian solo 2 variables sumando 100%, omitiendo var3 activa
        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":60},"
                + "{\"variableId\":%d,\"peso\":40}]}",
                var1.getId(), var2.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Fórmula de scoring inválida"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Faltan variables por incluir")));

        assertTrue(formulaScoreDAO.findAll().isEmpty());
    }

    @Test
    void rechazarVariablesDuplicadasEnSolicitud() throws Exception {
        VariableRiesgo var1 = crearVariable("Var1", TipoDatoVariable.NUMERICO, "50.0000", EstadoVariable.ACTIVA);

        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":50},"
                + "{\"variableId\":%d,\"peso\":50}]}",
                var1.getId(), var1.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Fórmula de scoring inválida"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("duplicada")));
    }

    @Test
    void rechazarVariableInexistente() throws Exception {
        VariableRiesgo var1 = crearVariable("Var1", TipoDatoVariable.NUMERICO, "50.0000", EstadoVariable.ACTIVA);

        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":50},"
                + "{\"variableId\":99999,\"peso\":50}]}",
                var1.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void desactivarVariableDeLaFormulaInactivaLaFormulaVigente() throws Exception {
        VariableRiesgo var1 = crearVariable("Ingresos", TipoDatoVariable.NUMERICO, "60.0000", EstadoVariable.ACTIVA);
        VariableRiesgo var2 = crearVariable("Egresos", TipoDatoVariable.NUMERICO, "40.0000", EstadoVariable.ACTIVA);

        // Crear formula vigente
        String cuerpo = String.format("{\"ponderaciones\":["
                + "{\"variableId\":%d,\"peso\":60},"
                + "{\"variableId\":%d,\"peso\":40}]}",
                var1.getId(), var2.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated());

        assertTrue(formulaScoreDAO.findByVigenteTrue().isPresent());

        // Inactivar var1 a traves del endpoint PATCH de variables (HU06)
        mockMvc.perform(patch("/api/variables/" + var1.getId() + "/estado")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk());

        // La formula ya no debe estar vigente para no quedar incompleta
        assertTrue(formulaScoreDAO.findByVigenteTrue().isEmpty());

        // Consultar la formula vigente debe devolver 404
        mockMvc.perform(get("/api/formula/vigente")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    // ---------- Validaciones Bean Validation (400)

    @Test
    void listaPonderacionesVaciaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ponderaciones\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"));
    }

    @Test
    void pesoInvalidoDevuelve400() throws Exception {
        VariableRiesgo var1 = crearVariable("Var1", TipoDatoVariable.NUMERICO, "100.0000", EstadoVariable.ACTIVA);

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ponderaciones\":[{\"variableId\":%d,\"peso\":0}]}", var1.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"));

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"ponderaciones\":[{\"variableId\":%d,\"peso\":100.12345}]}", var1.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"));
    }

    // ---------- Permisos y Roles

    @Test
    void analistaNoPuedeDefinirFormulaNiConsultarla() throws Exception {
        VariableRiesgo var1 = crearVariable("Var1", TipoDatoVariable.NUMERICO, "100.0000", EstadoVariable.ACTIVA);
        String cuerpo = String.format("{\"ponderaciones\":[{\"variableId\":%d,\"peso\":100}]}", var1.getId());

        mockMvc.perform(post("/api/formula")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/formula/vigente")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isForbidden());
    }

    @Test
    void peticionSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/formula")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ponderaciones\":[]}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/formula/vigente"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Helpers

    private VariableRiesgo crearVariable(String nombre, TipoDatoVariable tipoDato, String peso, EstadoVariable estado) {
        VariableRiesgo v = new VariableRiesgo(nombre, tipoDato, new BigDecimal(peso), estado);
        return variableRiesgoDAO.save(v);
    }

    private String obtenerToken(String email, String password) throws Exception {
        MockHttpServletRequestBuilder login = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        MvcResult result = mockMvc.perform(login).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
