package com.gpiv.atlanticsprinttech.backend.controlador;

import com.gpiv.atlanticsprinttech.backend.servicio.ServicioLoteGeoJSON;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.LoteGeoJSON;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.LotesGeoJSONCollection;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.DiagnosticoLotesGeoJSON;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para operaciones GeoJSON de Lotes.
 * Expone endpoints para exportar/importar lotes como GeoJSON.
 */
@RestController
@RequestMapping("/api/lotes/geojson")
public class ControladorLoteGeoJSON {

    private final ServicioLoteGeoJSON servicioLoteGeoJSON;

    public ControladorLoteGeoJSON(ServicioLoteGeoJSON servicioLoteGeoJSON) {
        this.servicioLoteGeoJSON = servicioLoteGeoJSON;
    }

    /**
     * GET /api/lotes/geojson
     * Devuelve todos los lotes como FeatureCollection GeoJSON.
     * Puede filtrar por: ?conGeometria=true para solo los que tienen geometría.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LotesGeoJSONCollection> listarComoGeoJSON(
        @RequestParam(defaultValue = "false") boolean conGeometria,
        Authentication autenticacion
    ) {
        var resultado = conGeometria
            ? servicioLoteGeoJSON.obtenerLotesConGeometriaComoGeoJSON()
            : servicioLoteGeoJSON.obtenerLotesComoGeoJSON();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(resultado);
    }

    /**
     * GET /api/lotes/geojson/diagnostico
     * Devuelve diagnóstico de completitud: cuántos lotes tienen geometría, etc.
     */
    @GetMapping("/diagnostico")
    public ResponseEntity<DiagnosticoLotesGeoJSON> diagnostico(Authentication autenticacion) {
        var diagnostico = servicioLoteGeoJSON.diagnosticarCompletitud();
        return ResponseEntity.ok(diagnostico);
    }

    /**
     * PUT /api/lotes/geojson/:id
     * Actualiza geometría de un lote (recibe WKT o coordenadas).
     */
    @PutMapping("/{id}/geometria")
    public ResponseEntity<String> actualizarGeometria(
        @PathVariable Long id,
        @RequestBody Map<String, String> body,
        Authentication autenticacion
    ) {
        String geomWKT = body.get("geom");
        if (geomWKT == null || geomWKT.isBlank()) {
            return ResponseEntity.badRequest().body("Campo 'geom' requerido (WKT format)");
        }
        servicioLoteGeoJSON.actualizarGeometria(id, geomWKT);
        return ResponseEntity.ok("{\"status\": \"Geometría actualizada\"}");
    }

    /**
     * POST /api/lotes/geojson/sincronizar
     * Sincroniza un Feature GeoJSON con un Lote existente (para bulk updates).
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<String> sincronizar(
        @Valid @RequestBody LoteGeoJSON feature,
        Authentication autenticacion
    ) {
        servicioLoteGeoJSON.sincronizarDesdeGeoJSON(feature);
        return ResponseEntity.ok("{\"status\": \"Lote sincronizado\"}");
    }
}


