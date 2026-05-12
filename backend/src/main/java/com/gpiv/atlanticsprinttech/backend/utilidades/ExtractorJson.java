package com.gpiv.atlanticsprinttech.backend.utilidades;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ExtractorJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExtractorJson() {}

    public static Integer extraerNecesidadMetrosCuadrados(String json) {
        if (json == null) return null;
        try {
            JsonNode nodo = MAPPER.readTree(json);
            JsonNode campo = nodo.get("necesidadMetrosCuadrados");
            return (campo != null && !campo.isNull()) ? campo.asInt() : null;
        } catch (Exception e) {
            return null;
        }
    }
}