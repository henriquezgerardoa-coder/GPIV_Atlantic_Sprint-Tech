package com.gpiv.atlanticsprinttech.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utilidad para extraer informacion de red desde el contexto HTTP actual.
 * Centraliza la logica de obtencion de IP para todos los servicios.
 */
public final class UtilRed {

    private static final String IP_DESCONOCIDA = "DESCONOCIDA";
    private static final String CABECERA_FORWARDED = "X-Forwarded-For";

    private UtilRed() {
    }

    /**
     * Obtiene la IP del cliente desde la peticion HTTP actual.
     * Considera X-Forwarded-For para entornos con proxy inverso.
     *
     * @return IP del cliente, o {@code "DESCONOCIDA"} si no hay contexto HTTP disponible
     */
    public static String obtenerIpActual() {
        try {
            ServletRequestAttributes atributos =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (atributos == null) {
                return IP_DESCONOCIDA;
            }
            HttpServletRequest peticion = atributos.getRequest();
            String forwarded = peticion.getHeader(CABECERA_FORWARDED);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return peticion.getRemoteAddr();
        } catch (Exception ignorada) {
            return IP_DESCONOCIDA;
        }
    }
}

