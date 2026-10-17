package com.udea.ScoreCrediticio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.udea.ScoreCrediticio.DAOs.RangoRiesgoDAO;

@SpringBootTest
@AutoConfigureMockMvc
class RangoRiesgoIntegrationTest {

    private static final String MENSAJE_GUARDADO =
            "Escala de niveles de riesgo guardada con \u00e9xito";
    private static final String MENSAJE_SOLAPE =
            "Los rangos num\u00e9ricos no pueden solaparse entre s\u00ed";

    // Escala continua de referencia: Bajo 701-1000, Medio 301-700, Alto 0-300.
    private static final String ESCALA_CONTINUA = "{"
            + "\"rangos\":["
            + "{\"nivel\":\"Bajo\",\"min\":701,\"max\":1000},"
            + "{\"nivel\":\"Medio\",\"min\":301,\"max\":700},"
            + "{\"nivel\":\"Alto\",\"min\":0,\"max\":300}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RangoRiesgoDAO rangoRiesgoDAO;

    private String tokenAdmin;
    private String tokenAnalista;

    @BeforeEach
    void preparar() throws Exception {
        rangoRiesgoDAO.deleteAll();
        tokenAdmin = obtenerToken("admin@score.local", "Admin123*");
        tokenAnalista = obtenerToken("analista@score.local", "Analista123*");
    }

    // ---------- Criterio de Aceptacion 1: guardado exitoso de rangos continuos

    @Test
    void administradorGuardaEscalaContinuaYSeMuestraMensajeDeExito() throws Exception {
        mockMvc.perform(guardar(tokenAdmin, ESCALA_CONTINUA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_GUARDADO))
                .andExpect(jsonPath("$.rangos.length()").value(3))
                .andExpect(jsonPath("$.rangos[0].nivel").value("Bajo"))
                .andExpect(jsonPath("$.rangos[0].min").value(701))
                .andExpect(jsonPath("$.rangos[0].max").value(1000))
                .andExpect(jsonPath("$.rangos[1].nivel").value("Medio"))
                .andExpect(jsonPath("$.rangos[1].min").value(301))
                .andExpect(jsonPath("$.rangos[1].max").value(700))
                .andExpect(jsonPath("$.rangos[2].nivel").value("Alto"))
                .andExpect(jsonPath("$.rangos[2].min").value(0))
                .andExpect(jsonPath("$.rangos[2].max").value(300));

        assertEquals(3, rangoRiesgoDAO.count());
    }

    @Test
    void aceptaLaEscalaConLasTresFranjasContiguasDeBordeACeroYMil() throws Exception {
        String cuerpo = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Bajo\",\"min\":334,\"max\":1000},"
                + "{\"nivel\":\"Medio\",\"min\":1,\"max\":333},"
                + "{\"nivel\":\"Alto\",\"min\":0,\"max\":0}]}";

        mockMvc.perform(guardar(tokenAdmin, cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rangos.length()").value(3));

        assertEquals(3, rangoRiesgoDAO.count());
    }

    @Test
    void consultarEscalaVigenteDevuelveLosRangosGuardados() throws Exception {
        mockMvc.perform(guardar(tokenAdmin, ESCALA_CONTINUA)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/rangos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Escala de niveles de riesgo vigente"))
                .andExpect(jsonPath("$.rangos.length()").value(3))
                .andExpect(jsonPath("$.rangos[0].nivel").value("Bajo"))
                .andExpect(jsonPath("$.rangos[2].nivel").value("Alto"));
    }

    @Test
    void consultarEscalaSinConfigurarDevuelve404() throws Exception {
        mockMvc.perform(get("/api/rangos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void guardarUnaNuevaEscalaReemplazaLaAnterior() throws Exception {
        mockMvc.perform(guardar(tokenAdmin, ESCALA_CONTINUA)).andExpect(status().isCreated());

        String nueva = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Bajo\",\"min\":501,\"max\":1000},"
                + "{\"nivel\":\"Medio\",\"min\":201,\"max\":500},"
                + "{\"nivel\":\"Alto\",\"min\":0,\"max\":200}]}";

        mockMvc.perform(guardar(tokenAdmin, nueva)).andExpect(status().isCreated());

        // Solo existe una escala vigente: siguen siendo 3 filas.
        assertEquals(3, rangoRiesgoDAO.count());

        mockMvc.perform(get("/api/rangos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rangos[0].min").value(501))
                .andExpect(jsonPath("$.rangos[1].min").value(201))
                .andExpect(jsonPath("$.rangos[2].min").value(0));
    }

    // ---------- Criterio de aceptacion 2: solapamiento o traslape entre rangos

    @Test
    void rangosSolapadosBloqueanElGuardadoYResaltanLosCampos() throws Exception {
        // Alto 0-400 y Medio 350-700 se cruzan.
        String cuerpo = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Bajo\",\"min\":701,\"max\":1000},"
                + "{\"nivel\":\"Medio\",\"min\":350,\"max\":700},"
                + "{\"nivel\":\"Alto\",\"min\":0,\"max\":400}]}";

        mockMvc.perform(guardar(tokenAdmin, cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Rangos solapados"))
                .andExpect(jsonPath("$.detail").value(MENSAJE_SOLAPE))
                .andExpect(jsonPath("$.camposConflicto").isArray())
                .andExpect(jsonPath("$.camposConflicto", org.hamcrest.Matchers.hasItem("Alto-max")))
                .andExpect(jsonPath("$.camposConflicto", org.hamcrest.Matchers.hasItem("Medio-min")));

        assertEquals(0, rangoRiesgoDAO.count());
    }

    @Test
    void solapamientoConLosDosLimitesExactosTambienSeBloquea() throws Exception {
        // Alto 0-400 y Medio 400-700 comparten el 400.
        String cuerpo = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Bajo\",\"min\":701,\"max\":1000},"
                + "{\"nivel\":\"Medio\",\"min\":400,\"max\":700},"
                + "{\"nivel\":\"Alto\",\"min\":0,\"max\":400}]}";

        mockMvc.perform(guardar(tokenAdmin, cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MENSAJE_SOLAPE));

        assertEquals(0, rangoRiesgoDAO.count());
    }

    // ---------- Criterio de aceptacion 3: brechas o vacios en la escala

    @Test
    void brechaEnLaEscalaBloqueaElGuardadoEInformaElIntervalo() throws Exception {
        // Alto 0-300, Medio 400-1000: queda 301-399 sin cobertura.
        String cuerpo = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Medio\",\"min\":400,\"max\":1000},"
                + "{\"nivel\":\"Alto\",\"min\":0,\"max\":300}]}";

        mockMvc.perform(guardar(tokenAdmin, cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Brecha en la escala de riesgo"))
                .andExpect(jsonPath("$.intervalosSinCobertura").isArray())
                .andExpect(jsonPath("$.intervalosSinCobertura", org.hamcrest.Matchers.hasItem("301-399")));

        assertEquals(0, rangoRiesgoDAO.count());
    }

    @Test
    void brechaAlInicioDeLaEscalaInformaElIntervaloCero() throws Exception {
        // Alto 1-50, Medio 51-100, Bajo 101-1000: queda el 0 sin cobertura.
        String cuerpo = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Bajo\",\"min\":101,\"max\":1000},"
                + "{\"nivel\":\"Medio\",\"min\":51,\"max\":100},"
                + "{\"nivel\":\"Alto\",\"min\":1,\"max\":50}]}";

        mockMvc.perform(guardar(tokenAdmin, cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Brecha en la escala de riesgo"))
                .andExpect(jsonPath("$.intervalosSinCobertura", org.hamcrest.Matchers.hasItem("0-0")));

        assertEquals(0, rangoRiesgoDAO.count());
    }

    @Test
    void brechaAlFinalDeLaEscalaBloqueaElGuardado() throws Exception {
        // Bajo 701-900, Medio 301-700, Alto 0-300: queda 901-1000 sin cobertura.
        String cuerpo = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Bajo\",\"min\":701,\"max\":900},"
                + "{\"nivel\":\"Medio\",\"min\":301,\"max\":700},"
                + "{\"nivel\":\"Alto\",\"min\":0,\"max\":300}]}";

        mockMvc.perform(guardar(tokenAdmin, cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.intervalosSinCobertura", org.hamcrest.Matchers.hasItem("901-1000")));

        assertEquals(0, rangoRiesgoDAO.count());
    }

    // ---------- Clasificacion automatica del puntaje

    @Test
    void clasificaElPuntajeEnElNivelCorrespondiente() throws Exception {
        mockMvc.perform(guardar(tokenAdmin, ESCALA_CONTINUA)).andExpect(status().isCreated());

        clasificar(tokenAdmin, 1000, "Bajo");
        clasificar(tokenAdmin, 701, "Bajo");
        clasificar(tokenAdmin, 700, "Medio");
        clasificar(tokenAdmin, 301, "Medio");
        clasificar(tokenAdmin, 300, "Alto");
        clasificar(tokenAdmin, 0, "Alto");
    }

    @Test
    void clasificarSinEscalaConfiguradaDevuelve404() throws Exception {
        mockMvc.perform(get("/api/rangos/clasificar?puntaje=500")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void clasificarPuntajeFueraDeLaEscalaDevuelve400() throws Exception {
        mockMvc.perform(guardar(tokenAdmin, ESCALA_CONTINUA)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/rangos/clasificar?puntaje=1001")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Configuraci\u00f3n de rangos inv\u00e1lida"));

        mockMvc.perform(get("/api/rangos/clasificar?puntaje=-1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest());
    }

    // ---------- Validaciones de estructura

    @Test
    void faltanNivelesObligatoriosDevuelve400() throws Exception {
        // Escala continua pero sin el nivel Alto: cobertura invalida.
        String cuerpo = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Bajo\",\"min\":501,\"max\":1000},"
                + "{\"nivel\":\"Medio\",\"min\":0,\"max\":500}]}";

        mockMvc.perform(guardar(tokenAdmin, cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Configuraci\u00f3n de rangos inv\u00e1lida"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Alto")));

        assertEquals(0, rangoRiesgoDAO.count());
    }

    @Test
    void nivelDuplicadoDevuelve400() throws Exception {
        String cuerpo = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Bajo\",\"min\":701,\"max\":1000},"
                + "{\"nivel\":\"Bajo\",\"min\":301,\"max\":700},"
                + "{\"nivel\":\"Medio\",\"min\":0,\"max\":300}]}";

        mockMvc.perform(guardar(tokenAdmin, cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Configuraci\u00f3n de rangos inv\u00e1lida"));

        assertEquals(0, rangoRiesgoDAO.count());
    }

    @Test
    void minimoMayorQueMaximoDevuelve400() throws Exception {
        String cuerpo = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Bajo\",\"min\":1000,\"max\":701},"
                + "{\"nivel\":\"Medio\",\"min\":301,\"max\":700},"
                + "{\"nivel\":\"Alto\",\"min\":0,\"max\":300}]}";

        mockMvc.perform(guardar(tokenAdmin, cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Configuraci\u00f3n de rangos inv\u00e1lida"));

        assertEquals(0, rangoRiesgoDAO.count());
    }

    @Test
    void puntajeFueraDeRangoEnLaSolicitudDevieneErrorDeValidacion() throws Exception {
        String cuerpo = "{"
                + "\"rangos\":["
                + "{\"nivel\":\"Bajo\",\"min\":701,\"max\":1001},"
                + "{\"nivel\":\"Medio\",\"min\":301,\"max\":700},"
                + "{\"nivel\":\"Alto\",\"min\":-1,\"max\":300}]}";

        mockMvc.perform(guardar(tokenAdmin, cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validaci\u00f3n"));

        assertEquals(0, rangoRiesgoDAO.count());
    }

    @Test
    void listaDeRangosVaciaDevuelve400() throws Exception {
        mockMvc.perform(guardar(tokenAdmin, "{\"rangos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validaci\u00f3n"));
    }

    // ---------- Permisos: el modulo es solo del ADMINISTRADOR

    @Test
    void elAnalistaNoPuedeGestionarLosRangos() throws Exception {
        mockMvc.perform(guardar(tokenAnalista, ESCALA_CONTINUA))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/rangos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/rangos/clasificar?puntaje=650")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isForbidden());
    }

    @Test
    void peticionesSinTokenDevuelven401() throws Exception {
        mockMvc.perform(post("/api/rangos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ESCALA_CONTINUA))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/rangos")).andExpect(status().isUnauthorized());
    }

    // ---------- Helpers

    private void clasificar(String token, int puntaje, String nivelEsperado) throws Exception {
        mockMvc.perform(get("/api/rangos/clasificar?puntaje=" + puntaje)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntaje").value(puntaje))
                .andExpect(jsonPath("$.nivel").value(nivelEsperado));
    }

    private MockHttpServletRequestBuilder guardar(String token, String cuerpo) {
        MockHttpServletRequestBuilder peticion = post("/api/rangos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);

        return token == null ? peticion : peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String obtenerToken(String email, String password) throws Exception {
        MockHttpServletRequestBuilder login = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        MvcResult result = mockMvc.perform(login).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
