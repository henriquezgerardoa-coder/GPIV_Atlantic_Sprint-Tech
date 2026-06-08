package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.LoteGeoJSON;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.LotesGeoJSONCollection;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.DiagnosticoLotesGeoJSON;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import java.util.List;
import java.util.Map;

/**
 * Servicio para convertir Lotes a GeoJSON y viceversa.
 * Facilita la sincronización entre geometrías de mapa y registros de lotes.
 */
public interface ServicioLoteGeoJSON {

    /**
     * Convierte un Lote a Feature GeoJSON.
     */
    LoteGeoJSON aGeoJSON(Lote lote);

    /**
     * Convierte una lista de Lotes a FeatureCollection GeoJSON.
     */
    LotesGeoJSONCollection aLotesGeoJSON(List<Lote> lotes);

    /**
     * Trae todos los lotes como GeoJSON (pagado o completo).
     */
    LotesGeoJSONCollection obtenerLotesComoGeoJSON();

    /**
     * Trae solo lotes con geometría específica (excluyendo nulos).
     */
    LotesGeoJSONCollection obtenerLotesConGeometriaComoGeoJSON();

    /**
     * Diagnostica completitud de geometrías en la BD.
     */
    DiagnosticoLotesGeoJSON diagnosticarCompletitud();

    /**
     * Actualiza la geometría de un lote desde WKT/GeoJSON.
     */
    void actualizarGeometria(Long loteId, String geomWKT);

    /**
     * Sincroniza un lote desde un Feature GeoJSON (actualiza propiedades y geometría).
     */
    void sincronizarDesdeGeoJSON(LoteGeoJSON feature);
}

