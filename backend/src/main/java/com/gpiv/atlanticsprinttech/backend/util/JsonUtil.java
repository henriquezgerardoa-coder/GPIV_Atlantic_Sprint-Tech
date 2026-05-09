package com.gpiv.atlanticsprinttech.backend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {}

    public static Integer extraerNecesidadMetrosCuadrados(String json) {
        if (json == null) return null;
        try {
            JsonNode node = MAPPER.readTree(json);
            JsonNode campo = node.get("necesidadMetrosCuadrados");
            return (campo != null && !campo.isNull()) ? campo.asInt() : null;
        } catch (Exception e) {
            return null;
        }
    }
}