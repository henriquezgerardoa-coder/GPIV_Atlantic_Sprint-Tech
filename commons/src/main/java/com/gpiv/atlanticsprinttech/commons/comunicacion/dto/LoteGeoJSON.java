package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.Map;

/**
 * DTO para representar un Lote como Feature GeoJSON.
 * Permite serializar/deserializar lotes con su geometría.
 */
public record LoteGeoJSON(
    String type,
    Map<String, Object> geometry,
    Map<String, Object> properties
) {
    public static LoteGeoJSON create(
        Long loteId,
        String codigo,
        Double superficie,
        boolean ocupado,
        String estado,
        String zona,
        Long empresaId,
        String nombreEmpresa,
        Map<String, Object> geom,
        Map<String, Object> customProps
    ) {
        var props = new java.util.LinkedHashMap<String, Object>();
        props.put("id", loteId);
        props.put("codigo", codigo);
        props.put("superficie_m2", superficie);
        props.put("ocupado", ocupado);
        props.put("estado_asignacion", estado);
        props.put("zona", zona);
        props.put("empresa_id", empresaId);
        props.put("nombre_empresa", nombreEmpresa);
        if (customProps != null) {
            props.putAll(customProps);
        }

        return new LoteGeoJSON("Feature", geom != null ? geom : defaultPoint(), props);
    }

    private static Map<String, Object> defaultPoint() {
        return Map.of(
            "type", "Point",
            "coordinates", new double[]{0, 0}
        );
    }
}

