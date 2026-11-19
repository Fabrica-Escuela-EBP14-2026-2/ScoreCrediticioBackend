package com.udea.ScoreCrediticio.Security;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Sanitiza los nombres durante la deserializacion del JSON, antes de que
 * Bean Validation evalue @NotBlank y @Size.
 */
public class NombreSanitizadoDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        return SanitizadorTexto.limpiarNombre(parser.getValueAsString());
    }
}
