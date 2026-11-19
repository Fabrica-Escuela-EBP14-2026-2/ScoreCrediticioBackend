package com.udea.ScoreCrediticio.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SanitizadorTextoTest {

    @Test
    void eliminaScriptYConservaElTextoPlano() {
        assertEquals("Ana", SanitizadorTexto.limpiarNombre("<script>alert('xss')</script>Ana"));
    }

    @Test
    void eliminaEtiquetasHtml() {
        assertEquals("Pérez Gómez", SanitizadorTexto.limpiarNombre("Pérez <b>Gómez</b>"));
    }

    @Test
    void neutralizaPalabrasClaveYComentariosSql() {
        assertEquals("Ana usuarios", SanitizadorTexto.limpiarNombre("Ana SELECT * FROM usuarios"));
        assertEquals("Robert solicitante",
                SanitizadorTexto.limpiarNombre("Robert'); DROP TABLE solicitante;--"));
    }

    @Test
    void conservaApostrofesGuionesYAcentos() {
        assertEquals("O'Brien", SanitizadorTexto.limpiarNombre("O'Brien"));
        assertEquals("María-José", SanitizadorTexto.limpiarNombre("María-José"));
        assertEquals("Ñandú", SanitizadorTexto.limpiarNombre("Ñandú"));
    }

    @Test
    void eliminaDigitosYPuntuacionNoPermitidaEnNombres() {
        assertEquals("Ana", SanitizadorTexto.limpiarNombre("Ana123 <test>"));
        assertEquals("Ana María", SanitizadorTexto.limpiarNombre("Ana & María"));
    }

    @Test
    void colapsaEspaciosYRecorta() {
        assertEquals("Ana María", SanitizadorTexto.limpiarNombre("  Ana   María  "));
    }

    @Test
    void unNombreQueQuedaVacioSeDevuelveVacio() {
        assertEquals("", SanitizadorTexto.limpiarNombre("<script></script>"));
        assertEquals("", SanitizadorTexto.limpiarNombre("12345"));
    }

    @Test
    void nuloSigueSiendoNulo() {
        assertNull(SanitizadorTexto.limpiarNombre(null));
        assertNull(SanitizadorTexto.limpiarTexto(null));
    }

    @Test
    void limpiarTextoConservaDigitosYPuntuacionComun() {
        assertEquals("Observación: hola 123 x",
                SanitizadorTexto.limpiarTexto("Observación: <b>hola</b> 123; DROP TABLE x"));
    }
}
