package com.udea.ScoreCrediticio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@AutoConfigureMockMvc
class AutenticacionIntegrationTest {

    private static final String MENSAJE_CREDENCIALES =
            "Usuario o contraseña incorrectos. Por favor intente nuevamente";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginConCredencialesValidasRetornaTokenYRol() throws Exception {
        mockMvc.perform(login("admin@score.local", "Admin123*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn").value(3600000))
                .andExpect(jsonPath("$.email").value("admin@score.local"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    void loginConPasswordIncorrectaRetornaMensajeExacto() throws Exception {
        mockMvc.perform(login("admin@score.local", "clave-mala"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(MENSAJE_CREDENCIALES));
    }

    @Test
    void loginConEmailNoRegistradoRetornaMensajeExacto() throws Exception {
        mockMvc.perform(login("nadie@score.local", "Admin123*"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(MENSAJE_CREDENCIALES));
    }

    @Test
    void endpointProtegidoSinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/perfil-financiero")
                        .param("tipoDocumento", "CC")
                        .param("numeroDocumento", "12345678"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analistaNoPuedeAccederARutasAdministrativas() throws Exception {
        String token = obtenerToken("analista@score.local", "Analista123*");

        mockMvc.perform(get("/api/variables/cualquiera")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorPasaElGuardDeRutasAdministrativas() throws Exception {
        String token = obtenerToken("admin@score.local", "Admin123*");

        mockMvc.perform(get("/api/variables/cualquiera")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void analistaPuedeUsarEndpointsDeNegocio() throws Exception {
        String token = obtenerToken("analista@score.local", "Analista123*");

        mockMvc.perform(get("/api/perfil-financiero")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private String obtenerToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(login(email, password))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
