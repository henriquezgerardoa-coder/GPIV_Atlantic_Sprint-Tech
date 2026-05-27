package com.gpiv.atlanticsprinttech.backend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static <T> T leer(ObjectMapper mapper, String json, Class<T> tipo) {
        try {
            return mapper.readValue(json, tipo);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer JSON: " + tipo.getSimpleName());
        }
    }

    public static <T> T leer(ObjectMapper mapper, String json, TypeReference<T> tipo) {
        try {
            return mapper.readValue(json, tipo);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer JSON");
        }
    }

    public static String escribir(ObjectMapper mapper, Object valor) {
        try {
            return mapper.writeValueAsString(valor);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al serializar JSON");
        }
    }
}
