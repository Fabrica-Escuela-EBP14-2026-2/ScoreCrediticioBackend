package com.udea.ScoreCrediticio.Security;

import java.text.Normalizer;
import java.util.regex.Pattern;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Limpieza centralizada de texto libre antes de persistirlo.
 *
 * <p>El sanitizador HTML elimina etiquetas y contenido ejecutable. Encima de eso
 * se neutralizan comentarios y palabras clave SQL como defensa en profundidad
 * (JPA ya parametriza las consultas) y se normaliza el texto para que no queden
 * caracteres de control ni espacios repetidos.</p>
 */
public final class SanitizadorTexto {

    private static final PolicyFactory POLITICA_HTML = new HtmlPolicyBuilder().toFactory();

    private static final Pattern BLOQUES_PELIGROSOS = Pattern.compile(
            "(?is)<(script|style|iframe|object|embed|template|svg|math)[^>]*>.*?</\\1\\s*>");

    private static final Pattern COMENTARIOS_SQL = Pattern.compile("(?s)/\\*.*?\\*/|--[^\\n]*");

    private static final Pattern PALABRAS_SQL = Pattern.compile(
            "\\b(SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|UNION|EXEC|EXECUTE|MERGE|"
                    + "CALL|DECLARE|GRANT|REVOKE|FROM|WHERE|TABLE|DATABASE|VALUES|INTO|SET|JOIN|AND|OR|"
                    + "NOT|NULL|WAITFOR|SLEEP|BENCHMARK|XP_CMDSHELL|INFORMATION_SCHEMA|PG_SLEEP)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern OPERADORES = Pattern.compile("[;*=<>{}\\[\\]`$|\\\\]");

    private static final Pattern CARACTERES_NO_NOMBRE = Pattern.compile("[^\\p{L}\\p{M} '\\-.]");

    private static final Pattern APOSTROFE_SUELTO = Pattern.compile("(?<!\\p{L})'|'(?!\\p{L})");

    private static final Pattern CONTROLES = Pattern.compile("\\p{Cntrl}");

    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private SanitizadorTexto() {
    }

    /**
     * Limpieza para nombres y apellidos: solo letras Unicode, marcas, espacios,
     * apóstrofes entre letras, guiones y puntos.
     */
    public static String limpiarNombre(String valor) {
        String limpio = limpiarBase(valor);
        if (limpio == null) {
            return null;
        }

        limpio = CARACTERES_NO_NOMBRE.matcher(limpio).replaceAll(" ");
        limpio = APOSTROFE_SUELTO.matcher(limpio).replaceAll(" ");
        return normalizarEspacios(limpio);
    }

    /**
     * Limpieza para textos libres futuros (por ejemplo observaciones). Conserva
     * dígitos y puntuación común, pero elimina contenido ejecutable y controles.
     */
    public static String limpiarTexto(String valor) {
        String limpio = limpiarBase(valor);
        if (limpio == null) {
            return null;
        }

        limpio = CONTROLES.matcher(limpio).replaceAll(" ");
        return normalizarEspacios(limpio);
    }

    private static String limpiarBase(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = Normalizer.normalize(valor, Normalizer.Form.NFKC);
        limpio = BLOQUES_PELIGROSOS.matcher(limpio).replaceAll(" ");
        limpio = POLITICA_HTML.sanitize(limpio);
        // El sanitizador codifica apóstrofes y ampersands; se recuperan para no
        // convertir "O'Brien" en "O Brien" ni perder el texto original.
        limpio = decodificarEntidades(limpio);
        limpio = COMENTARIOS_SQL.matcher(limpio).replaceAll(" ");
        limpio = PALABRAS_SQL.matcher(limpio).replaceAll(" ");
        limpio = OPERADORES.matcher(limpio).replaceAll(" ");
        return limpio;
    }

    private static String decodificarEntidades(String valor) {
        return valor
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&apos;", "'")
                .replace("&#34;", "\"")
                .replace("&quot;", "\"")
                .replace("&amp;", "&");
    }

    private static String normalizarEspacios(String valor) {
        return ESPACIOS.matcher(valor).replaceAll(" ").trim();
    }
}
