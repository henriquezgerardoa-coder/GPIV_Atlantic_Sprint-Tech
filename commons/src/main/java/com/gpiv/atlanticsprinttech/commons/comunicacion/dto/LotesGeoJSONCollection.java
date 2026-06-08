package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO para representar una colección de Lotes como GeoJSON FeatureCollection.
 * Estándar GeoJSON RFC 7946.
 */
public class LotesGeoJSONCollection {
    public String type = "FeatureCollection";

    public Map<String, Object> crs = Map.of(
        "type", "name",
        "properties", Map.of("name", "EPSG:4326")
    );

    public List<LoteGeoJSON> features;

    public Map<String, Object> properties;

    public LotesGeoJSONCollection(List<LoteGeoJSON> features, Map<String, Object> properties) {
        this.features = features;
        this.properties = properties != null ? properties : Map.of(
            "total_lotes", features.size(),
            "generator", "GPIV-Atlantic",
            "generated_at", java.time.LocalDateTime.now().toString()
        );
    }

    public LotesGeoJSONCollection(List<LoteGeoJSON> features) {
        this(features, null);
    }
}

