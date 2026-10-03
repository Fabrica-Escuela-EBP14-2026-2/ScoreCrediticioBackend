package com.udea.ScoreCrediticio.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.udea.ScoreCrediticio.Model.TipoUsuario;
import com.udea.ScoreCrediticio.Model.Usuario;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-prueba-con-mas-de-32-caracteres-0123456789";

    private final JwtService jwtService = new JwtService(SECRETO, 3600000);

    @Test
    void generarYLeerToken() {
        Usuario usuario = new Usuario(1L, "admin@score.local", "hash", TipoUsuario.ADMINISTRADOR);

        String token = jwtService.generarToken(usuario);

        assertTrue(jwtService.esTokenValido(token));
        assertEquals("admin@score.local", jwtService.extraerEmail(token));
        assertEquals("ADMINISTRADOR", jwtService.extraerRol(token));
    }

    @Test
    void tokenFirmadoConOtraClaveEsInvalido() {
        JwtService otroServicio = new JwtService("otra-clave-de-prueba-con-mas-de-32-caracteres-999", 3600000);
        Usuario usuario = new Usuario(1L, "ana@test.com", "hash", TipoUsuario.ANALISTA);

        String token = otroServicio.generarToken(usuario);

        assertFalse(jwtService.esTokenValido(token));
    }

    @Test
    void tokenVencidoEsInvalido() {
        JwtService servicioVencido = new JwtService(SECRETO, -1000);
        Usuario usuario = new Usuario(1L, "ana@test.com", "hash", TipoUsuario.ANALISTA);

        String token = servicioVencido.generarToken(usuario);

        assertFalse(jwtService.esTokenValido(token));
    }

    @Test
    void tokenAlteradoEsInvalido() {
        Usuario usuario = new Usuario(1L, "ana@test.com", "hash", TipoUsuario.ANALISTA);
        String token = jwtService.generarToken(usuario);

        String alterado = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertFalse(jwtService.esTokenValido(alterado));
    }

    @Test
    void textoQueNoEsTokenEsInvalido() {
        assertFalse(jwtService.esTokenValido("esto-no-es-un-token"));
    }
}
