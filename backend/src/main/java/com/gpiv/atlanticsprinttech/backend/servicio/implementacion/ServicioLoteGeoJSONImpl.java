package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioLoteGeoJSON;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.LoteGeoJSON;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.LotesGeoJSONCollection;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.DiagnosticoLotesGeoJSON;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ServicioLoteGeoJSONImpl implements ServicioLoteGeoJSON {

    private final RepositorioLote repositorioLote;
    private final ObjectMapper objectMapper;

    public ServicioLoteGeoJSONImpl(RepositorioLote repositorioLote, ObjectMapper objectMapper) {
        this.repositorioLote = repositorioLote;
        this.objectMapper = objectMapper;
    }

    @Override
    public LoteGeoJSON aGeoJSON(Lote lote) {
        Map<String, Object> geom = null;
        if (lote.getGeom() != null) {
            try {
                // Si geom es WKT string, por ahora devolvemos punto por defecto
                // En producción, usar lib PostGIS/JTS para convertir WKT -> GeoJSON
                geom = new HashMap<>();
                geom.put("type", "Point");
                geom.put("coordinates", new double[]{0.0, 0.0}); // placeholder
            } catch (Exception e) {
                geom = null;
            }
        }

        Map<String, Object> customProps = null;
        if (lote.getProperties() != null) {
            try {
                customProps = objectMapper.readValue(lote.getProperties(), Map.class);
            } catch (Exception e) {
                customProps = new HashMap<>();
            }
        }

        return LoteGeoJSON.create(
            lote.getId(),
            lote.getCodigo(),
            lote.getSuperficieMetrosCuadrados(),
            lote.isOcupado(),
            lote.getNombreEtapa(),
            lote.getZona(),
            lote.getEmpresaId(),
            lote.getNombreEmpresa(),
            geom,
            customProps
        );
    }

    @Override
    public LotesGeoJSONCollection aLotesGeoJSON(List<Lote> lotes) {
        var features = lotes.stream().map(this::aGeoJSON).toList();
        var stats = new HashMap<String, Object>();
        stats.put("total_lotes", lotes.size());
        stats.put("lotes_con_geometria", lotes.stream().filter(l -> l.getGeom() != null).count());
        return new LotesGeoJSONCollection(features, stats);
    }

    @Override
    public LotesGeoJSONCollection obtenerLotesComoGeoJSON() {
        var lotes = repositorioLote.findAllConEmpresa();
        return aLotesGeoJSON(lotes);
    }

    @Override
    public LotesGeoJSONCollection obtenerLotesConGeometriaComoGeoJSON() {
        var lotes = repositorioLote.findAllConGeometriaLoaded();
        return aLotesGeoJSON(lotes);
    }

    @Override
    public DiagnosticoLotesGeoJSON diagnosticarCompletitud() {
        long total = repositorioLote.count();
        long conGeom = total - repositorioLote.countByGeomIsNull();
        long sinGeom = total - conGeom;

        // Buscar lotes con sub-lotes
        var lotesPrincipales = repositorioLote.findAllLotesPrincipales();
        long conSub = lotesPrincipales.stream().filter(Lote::esSubdividido).count();

        return DiagnosticoLotesGeoJSON.crear(total, conGeom, sinGeom, conSub, 0);
    }

    @Override
    public void actualizarGeometria(Long loteId, String geomWKT) {
        if (!repositorioLote.existsById(loteId)) return;
        repositorioLote.actualizarGeom(loteId, geomWKT);
    }

    @Override
    public void sincronizarDesdeGeoJSON(LoteGeoJSON feature) {
        if (feature.properties() == null || !feature.properties().containsKey("id")) return;
        Long loteId = ((Number) feature.properties().get("id")).longValue();
        if (!repositorioLote.existsById(loteId)) return;
        try {
            String json = objectMapper.writeValueAsString(feature.properties());
            repositorioLote.actualizarProperties(loteId, json);
        } catch (Exception e) {
            // JSON serialization failed; skip silently
        }
    }
}

