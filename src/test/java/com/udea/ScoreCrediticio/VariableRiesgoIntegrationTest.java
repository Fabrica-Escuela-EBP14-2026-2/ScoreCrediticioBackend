package com.udea.ScoreCrediticio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.udea.ScoreCrediticio.DAOs.VariableRiesgoDAO;
import com.udea.ScoreCrediticio.Model.EstadoVariable;
import com.udea.ScoreCrediticio.Model.TipoDatoVariable;
import com.udea.ScoreCrediticio.Model.VariableRiesgo;

@SpringBootTest
@AutoConfigureMockMvc
class VariableRiesgoIntegrationTest {

    private static final String MENSAJE_PESO =
            "La suma de los pesos de las variables activas no puede superar el 100%. "
                    + "Ajuste el peso indicado para poder guardar la variable";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VariableRiesgoDAO variableRiesgoDAO;

    private String tokenAdmin;
    private String tokenAnalista;

    @BeforeEach
    void preparar() throws Exception {
        variableRiesgoDAO.deleteAll();
        tokenAdmin = obtenerToken("admin@score.local", "Admin123*");
        tokenAnalista = obtenerToken("analista@score.local", "Analista123*");
    }

    // ---------- Criterio de aceptacion 1: se crea y queda disponible para la formula

    @Test
    void administradorRegistraVariableYQuedaActiva() throws Exception {
        mockMvc.perform(crear(tokenAdmin, "Ingresos", "NUMERICO", "40"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Ingresos"))
                .andExpect(jsonPath("$.tipoDato").value("NUMERICO"))
                .andExpect(jsonPath("$.peso").value(40.0000))
                .andExpect(jsonPath("$.estado").value("Activa"))
                .andExpect(jsonPath("$.fechaRegistro").isNotEmpty());

        mockMvc.perform(get("/api/variables").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ingresos"));
    }

    @Test
    void administradorRegistraLosTresTiposDeDatoDeLaHistoria() throws Exception {
        mockMvc.perform(crear(tokenAdmin, "Edad", "NUMERICO", "25")).andExpect(status().isCreated());
        mockMvc.perform(crear(tokenAdmin, "DeudaIngresos", "PORCENTAJE", "25")).andExpect(status().isCreated());
        mockMvc.perform(crear(tokenAdmin, "Sector", "CATEGORICO", "20")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/variables").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void elNombreSeGuardaSinEspaciosSobrantes() throws Exception {
        mockMvc.perform(crear(tokenAdmin, "  Ingresos  ", "NUMERICO", "40"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Ingresos"));
    }

    // ---------- Criterio de aceptacion 2: la suma no puede superar 100%

    @Test
    void sumaDePesosActivosSuperando100DevuelveErrorYNoGuarda() throws Exception {
        mockMvc.perform(crear(tokenAdmin, "Ingresos", "NUMERICO", "60")).andExpect(status().isCreated());

        mockMvc.perform(crear(tokenAdmin, "Egresos", "NUMERICO", "55"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Suma de pesos excede el 100%"))
                .andExpect(jsonPath("$.detail").value(MENSAJE_PESO))
                .andExpect(jsonPath("$.pesoTotalActual").value("60%"))
                .andExpect(jsonPath("$.pesoSolicitado").value("55%"))
                .andExpect(jsonPath("$.pesoTotalResultante").value("115%"));

        mockMvc.perform(get("/api/variables").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void sumaExactaDe100SeAcepta() throws Exception {
        mockMvc.perform(crear(tokenAdmin, "Ingresos", "NUMERICO", "33.33")).andExpect(status().isCreated());
        mockMvc.perform(crear(tokenAdmin, "Egresos", "NUMERICO", "33.33")).andExpect(status().isCreated());
        mockMvc.perform(crear(tokenAdmin, "Antiguedad", "NUMERICO", "33.34")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/variables").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void unPesoDe100EnUnaSolaVariableSeAcepta() throws Exception {
        mockMvc.perform(crear(tokenAdmin, "TodoElScore", "NUMERICO", "100"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.peso").value(100.0000));
    }

    @Test
    void unPesoMayorASuPropioLimiteSeRechaza() throws Exception {
        mockMvc.perform(crear(tokenAdmin, "Ingresos", "NUMERICO", "100.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.peso").exists());

        mockMvc.perform(get("/api/variables").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void soloSumanLasVariablesActivas() throws Exception {
        // Una variable inactiva con peso alto no debe bloquear el catalogo.
        variableRiesgoDAO.save(new VariableRiesgo("Archivada", TipoDatoVariable.NUMERICO,
                new BigDecimal("90.0000"), EstadoVariable.INACTIVA));

        mockMvc.perform(crear(tokenAdmin, "Ingresos", "NUMERICO", "80")).andExpect(status().isCreated());
    }

    // ---------- Permisos: la funcion es del rol ADMINISTRADOR

    @Test
    void elAnalistaNoPuedeRegistrarVariables() throws Exception {
        mockMvc.perform(crear(tokenAnalista, "Ingresos", "NUMERICO", "40"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/variables").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/variables")).andExpect(status().isUnauthorized());

        // El intento del analista no dejo ninguna variable registrada.
        mockMvc.perform(get("/api/variables").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- Validaciones de entrada y duplicados

    @Test
    void nombreDuplicadoDevuelveConflicto() throws Exception {
        mockMvc.perform(crear(tokenAdmin, "Ingresos", "NUMERICO", "40")).andExpect(status().isCreated());

        mockMvc.perform(crear(tokenAdmin, "ingresos", "NUMERICO", "20"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe una variable registrada con este nombre"));
    }

    @Test
    void camposInvalidosDevienenErrorDeValidacion() throws Exception {
        mockMvc.perform(crear(tokenAdmin, "  ", "NUMERICO", "40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").exists());

        mockMvc.perform(crear(tokenAdmin, "PesoCero", "NUMERICO", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.peso").value("El peso debe ser mayor que 0"));

        mockMvc.perform(crear(tokenAdmin, "PesoNegativo", "NUMERICO", "-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.peso").exists());

        mockMvc.perform(crear(tokenAdmin, "PesoExcesivo", "NUMERICO", "20.123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.peso").exists());

        mockMvc.perform(crear(tokenAdmin, "TipoInexistente", "TEXTO", "10"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(crear(tokenAdmin, "SinPeso", "NUMERICO", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.peso").exists());
    }

    private MockHttpServletRequestBuilder crear(String token, String nombre, String tipoDato, String peso) {
        String cuerpo = "{\"nombre\":\"" + nombre + "\",\"tipoDato\":\"" + tipoDato + "\""
                + (peso == null ? "" : ",\"peso\":" + peso) + "}";

        return post("/api/variables")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    private String obtenerToken(String email, String password) throws Exception {
        MockHttpServletRequestBuilder login = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        MvcResult result = mockMvc.perform(login).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
