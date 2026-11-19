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
import com.udea.ScoreCrediticio.DAOs.HistorialEvaluacionDAO;
import com.udea.ScoreCrediticio.DAOs.PerfilFinancieroDAO;
import com.udea.ScoreCrediticio.DAOs.SolicitanteDAO;
import com.udea.ScoreCrediticio.Model.Solicitante;

@SpringBootTest
@AutoConfigureMockMvc
class SolicitanteSeguridadIntegrationTest {

    private static final String MENSAJE_DUPLICADO =
            "El solicitante con este tipo y número de documento ya se encuentra registrado en la plataforma";

    private static final String TELEFONO_VALIDO = "3001234567";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitanteDAO solicitanteDAO;

    @Autowired
    private PerfilFinancieroDAO perfilFinancieroDAO;

    @Autowired
    private HistorialEvaluacionDAO historialEvaluacionDAO;

    private String tokenAnalista;

    @BeforeEach
    void preparar() throws Exception {
        historialEvaluacionDAO.deleteAll();
        perfilFinancieroDAO.deleteAll();
        solicitanteDAO.deleteAll();

        tokenAnalista = obtenerToken("analista@score.local", "Analista123*");
    }

    // ---------- Escenario 1: duplicado real en el servidor

    @Test
    void registroValidoDevuelve201YGuardaElSolicitante() throws Exception {
        mockMvc.perform(registrar(cuerpoValido("9001")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipoDocumento").value("CC"))
                .andExpect(jsonPath("$.numeroDocumento").value("9001"))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Pérez"));

        assertEquals(1, solicitanteDAO.count());
    }

    @Test
    void duplicadoExactoDevuelve409ConMensajeYCampoDeDocumento() throws Exception {
        mockMvc.perform(registrar(cuerpoValido("9002")))
                .andExpect(status().isCreated());

        mockMvc.perform(registrar(cuerpoValido("9002")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"))
                .andExpect(jsonPath("$.detail").value(MENSAJE_DUPLICADO))
                .andExpect(jsonPath("$.campo").value("numeroDocumento"));

        assertEquals(1, solicitanteDAO.count());
    }

    @Test
    void mismoNumeroConOtroTipoDocumentoSePermite() throws Exception {
        mockMvc.perform(registrar(cuerpo("CC", "9003", "Ana", "Pérez",
                        TELEFONO_VALIDO, "ana@test.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(registrar(cuerpo("CE", "9003", "Ana", "Pérez",
                        TELEFONO_VALIDO, "ana@test.com")))
                .andExpect(status().isCreated());

        assertEquals(2, solicitanteDAO.count());
    }

    // ---------- Escenario 2: sanitizacion y limpieza

    @Test
    void sanitizaNombreYApellidoAntesDePersistir() throws Exception {
        String cuerpo = cuerpo("CC", "9004",
                "<script>alert('xss')</script>Ana",
                "Pérez <b>Gómez</b>",
                TELEFONO_VALIDO, "ana@test.com");

        mockMvc.perform(registrar(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Pérez Gómez"));

        Solicitante guardado = solicitanteDAO.findAll().get(0);
        assertEquals("Ana", guardado.getNombre());
        assertEquals("Pérez Gómez", guardado.getApellido());
    }

    @Test
    void neutralizaIntentoDeInyeccionSqlYGuardaTextoSeguro() throws Exception {
        String cuerpo = cuerpo("CC", "9005",
                "Ana SELECT * FROM usuarios",
                "Robert'); DROP TABLE solicitante;--",
                TELEFONO_VALIDO, "ana@test.com");

        mockMvc.perform(registrar(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Ana usuarios"))
                .andExpect(jsonPath("$.apellido").value("Robert solicitante"));

        Solicitante guardado = solicitanteDAO.findAll().get(0);
        assertEquals("Ana usuarios", guardado.getNombre());
        assertEquals("Robert solicitante", guardado.getApellido());
        assertEquals(1, solicitanteDAO.count());
    }

    @Test
    void nombreQueQuedaVacioTrasSanitizarDevuelve400() throws Exception {
        String cuerpo = cuerpo("CC", "9006", "<script></script>", "Pérez",
                TELEFONO_VALIDO, "ana@test.com");

        mockMvc.perform(registrar(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").isNotEmpty());

        assertEquals(0, solicitanteDAO.count());
    }

    // ---------- Escenario 3: formatos invalidos y montos no permitidos

    @Test
    void telefonoAceptaSieteYDiezDigitos() throws Exception {
        mockMvc.perform(registrar(cuerpo("CC", "9007", "Ana", "Pérez",
                        "3001234", "ana@test.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(registrar(cuerpo("CC", "9008", "Ana", "Pérez",
                        "3001234567", "ana@test.com")))
                .andExpect(status().isCreated());

        assertEquals(2, solicitanteDAO.count());
    }

    @Test
    void telefonoConOchoONueveDigitosDevuelve400() throws Exception {
        mockMvc.perform(registrar(cuerpo("CC", "9009", "Ana", "Pérez",
                        "30012345", "ana@test.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.telefono").isNotEmpty());

        mockMvc.perform(registrar(cuerpo("CC", "9010", "Ana", "Pérez",
                        "300123456", "ana@test.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.telefono").isNotEmpty());

        assertEquals(0, solicitanteDAO.count());
    }

    @Test
    void documentoConFormatoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(registrar(cuerpo("CC", "123 <script>", "Ana", "Pérez",
                        TELEFONO_VALIDO, "ana@test.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.numeroDocumento").isNotEmpty());

        assertEquals(0, solicitanteDAO.count());
    }

    @Test
    void ingresosYEgresosNegativosDevuelven400() throws Exception {
        mockMvc.perform(registrar(cuerpoValido("9011")))
                .andExpect(status().isCreated());

        mockMvc.perform(registrarPerfil("9011", "-1", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ingresos").isNotEmpty());

        mockMvc.perform(registrarPerfil("9011", "100", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.egresos").isNotEmpty());

        assertEquals(0, perfilFinancieroDAO.count());
    }

    // ---------- Verificacion previa y seguridad

    @Test
    void consultaExisteDevuelveTrueParaRegistradoYFalseParaNuevo() throws Exception {
        mockMvc.perform(registrar(cuerpoValido("9012")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/solicitantes/existe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista)
                        .param("tipoDocumento", "CC")
                        .param("numeroDocumento", "9012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true));

        mockMvc.perform(get("/api/solicitantes/existe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista)
                        .param("tipoDocumento", "CC")
                        .param("numeroDocumento", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false));
    }

    @Test
    void consultaExisteSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/solicitantes/existe")
                        .param("tipoDocumento", "CC")
                        .param("numeroDocumento", "9013"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultaExisteConParametrosInvalidosDevuelve400() throws Exception {
        mockMvc.perform(get("/api/solicitantes/existe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista)
                        .param("tipoDocumento", "CC")
                        .param("numeroDocumento", "123 <script>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.numeroDocumento").isNotEmpty());
    }

    // ---------- Helpers

    private MockHttpServletRequestBuilder registrar(String cuerpo) {
        return post("/api/solicitantes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    private MockHttpServletRequestBuilder registrarPerfil(String numeroDocumento, String ingresos,
            String egresos) {
        String cuerpo = "{\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"" + numeroDocumento
                + "\",\"ingresos\":" + ingresos + ",\"egresos\":" + egresos + "}";

        return post("/api/perfil-financiero")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnalista)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    private String cuerpoValido(String numeroDocumento) {
        return cuerpo("CC", numeroDocumento, "Ana", "Pérez", TELEFONO_VALIDO, "ana@test.com");
    }

    private String cuerpo(String tipoDocumento, String numeroDocumento, String nombre,
            String apellido, String telefono, String email) {
        return "{\"tipoDocumento\":\"" + tipoDocumento + "\","
                + "\"numeroDocumento\":\"" + numeroDocumento + "\","
                + "\"nombre\":\"" + nombre + "\","
                + "\"apellido\":\"" + apellido + "\","
                + "\"telefono\":\"" + telefono + "\","
                + "\"email\":\"" + email + "\"}";
    }

    private String obtenerToken(String email, String password) throws Exception {
        MockHttpServletRequestBuilder login = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        MvcResult result = mockMvc.perform(login).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
