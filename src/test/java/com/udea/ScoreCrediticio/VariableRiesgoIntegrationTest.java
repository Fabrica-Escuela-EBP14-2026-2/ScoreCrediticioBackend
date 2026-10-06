package com.udea.ScoreCrediticio;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    // ---------- Criterio de aceptacion 3: activar o inactivar sin eliminar la variable

    @Test
    void desactivarVariableLaDejaInactivaYVisibleEnElListado() throws Exception {
        Long id = crearYDevolverId(tokenAdmin, "Ingresos", "NUMERICO", "40");

        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Ingresos"))
                .andExpect(jsonPath("$.peso").value(40.0000))
                .andExpect(jsonPath("$.estado").value("Inactiva"))
                .andExpect(jsonPath("$.fechaRegistro").isNotEmpty());

        // No se elimina nada: sigue en el listado, con su id y su fecha de registro.
        mockMvc.perform(get("/api/variables").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].estado").value("Inactiva"));
    }

    @Test
    void desactivarVariableLaSacaDelCalculoDePesos() throws Exception {
        Long id = crearYDevolverId(tokenAdmin, "Ingresos", "NUMERICO", "80");

        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk());

        // Con la variable de 80 inactiva, la de 100 entra sin conflicto.
        mockMvc.perform(crear(tokenAdmin, "Egresos", "NUMERICO", "100")).andExpect(status().isCreated());
    }

    @Test
    void reactivarVariableVuelveAMarcarlaComoActiva() throws Exception {
        Long id = crearYDevolverId(tokenAdmin, "Ingresos", "NUMERICO", "40");

        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Inactiva"));

        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"Activa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Activa"));

        // Reactiva vuelve a contar para el calculo: 40 + 70 se pasa del 100%.
        mockMvc.perform(crear(tokenAdmin, "Egresos", "NUMERICO", "70"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.pesoTotalActual").value("40%"))
                .andExpect(jsonPath("$.pesoTotalResultante").value("110%"));
    }

    @Test
    void desactivarLiberaPresupuestoParaNuevasAltas() throws Exception {
        Long id = crearYDevolverId(tokenAdmin, "Ingresos", "NUMERICO", "80");

        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(crear(tokenAdmin, "Egresos", "NUMERICO", "80")).andExpect(status().isCreated());
        mockMvc.perform(crear(tokenAdmin, "Antiguedad", "NUMERICO", "30"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.pesoTotalActual").value("80%"));
    }

    @Test
    void reactivarVariableQueExcedeEl100PctDevuelve409() throws Exception {
        Long id = crearYDevolverId(tokenAdmin, "Ingresos", "NUMERICO", "30");
        mockMvc.perform(crear(tokenAdmin, "Egresos", "NUMERICO", "40")).andExpect(status().isCreated());
        mockMvc.perform(crear(tokenAdmin, "Antiguedad", "NUMERICO", "30")).andExpect(status().isCreated());

        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(crear(tokenAdmin, "Sector", "CATEGORICO", "30")).andExpect(status().isCreated());

        // Las activas ya suman 100%; reactivar devolveria el 30% de Ingresos: 130%.
        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"Activa\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Suma de pesos excede el 100%"))
                .andExpect(jsonPath("$.detail").value(MENSAJE_PESO))
                .andExpect(jsonPath("$.pesoTotalActual").value("100%"))
                .andExpect(jsonPath("$.pesoSolicitado").value("30%"))
                .andExpect(jsonPath("$.pesoTotalResultante").value("130%"));

        // El 409 no la deja activa a medias.
        assertEquals("Inactiva", estadoDe("Ingresos"));
    }

    @Test
    void repetirElMismoEstadoRespondeOkSinCambiarNada() throws Exception {
        Long id = crearYDevolverId(tokenAdmin, "Ingresos", "NUMERICO", "40");

        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Inactiva"));

        // Idempotente: un doble clic o un reintento del frontend no es un error.
        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Inactiva"));

        // Tampoco falla al reactivar una variable que ya estaba activa.
        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"ACTIVA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Activa"));
    }

    @Test
    void cambiarEstadoDeVariableInexistenteDevuelve404() throws Exception {
        mockMvc.perform(cambiarEstado(tokenAdmin, 9999L, "{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("Variable de riesgo con id 9999 no encontrada"));
    }

    @Test
    void elAnalistaNoPuedeCambiarElEstado() throws Exception {
        Long id = crearYDevolverId(tokenAdmin, "Ingresos", "NUMERICO", "40");

        mockMvc.perform(cambiarEstado(tokenAnalista, id, "{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(cambiarEstado(null, id, "{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isUnauthorized());

        // Ninguno de los dos intentos cambio el estado de la variable.
        mockMvc.perform(get("/api/variables").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(jsonPath("$[0].estado").value("Activa"));
    }

    @Test
    void estadoInvalidoDevuelve400() throws Exception {
        Long id = crearYDevolverId(tokenAdmin, "Ingresos", "NUMERICO", "40");

        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{\"estado\":\"Borrada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud malformada"));

        mockMvc.perform(cambiarEstado(tokenAdmin, id, "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"))
                .andExpect(jsonPath("$.errors.estado").value("El estado es obligatorio"));

        mockMvc.perform(cambiarEstado(tokenAdmin, id, ""))
                .andExpect(status().isBadRequest());

        // El id tiene que ser un numero.
        mockMvc.perform(patch("/api/variables/abc/estado")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"Inactiva\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.id").exists());

        mockMvc.perform(get("/api/variables").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(jsonPath("$[0].estado").value("Activa"));
    }

    private MockHttpServletRequestBuilder crear(String token, String nombre, String tipoDato, String peso) {
        String cuerpo = "{\"nombre\":\"" + nombre + "\",\"tipoDato\":\"" + tipoDato + "\""
                + (peso == null ? "" : ",\"peso\":" + peso) + "}";

        return post("/api/variables")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    private Long crearYDevolverId(String token, String nombre, String tipoDato, String peso) throws Exception {
        MvcResult result = mockMvc.perform(crear(token, nombre, tipoDato, peso))
                .andExpect(status().isCreated())
                .andReturn();

        // json-smart devuelve Integer en los ids chicos, hay que normalizarlo a Long.
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    // Estado persistido de una variable concreta, para comprobar que un 403/409 no la movio.
    private String estadoDe(String nombre) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/variables")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();

        List<String> estados = JsonPath.read(result.getResponse().getContentAsString(),
                "$[?(@.nombre == '" + nombre + "')].estado");

        return estados.isEmpty() ? null : estados.get(0);
    }

    // cuerpo va en crudo a proposito: los tests de 400 mandan estados invalidos o incompletos.
    private MockHttpServletRequestBuilder cambiarEstado(String token, Long id, String cuerpo) {
        MockHttpServletRequestBuilder peticion = patch("/api/variables/" + id + "/estado")
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
