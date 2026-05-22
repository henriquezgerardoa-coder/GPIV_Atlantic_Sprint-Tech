package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convierte valores legacy de estado_asignacion almacenados en la DB al nuevo enum.
 * Garantiza compatibilidad hacia atrás sin necesitar migración de datos.
 */
@Converter
public class EstadoAsignacionLoteConverter implements AttributeConverter<EstadoAsignacionLote, String> {

    @Override
    public String convertToDatabaseColumn(EstadoAsignacionLote attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public EstadoAsignacionLote convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        return switch (dbData) {
            case "ADJUDICADO_RADICADA" -> EstadoAsignacionLote.ADJUDICADO;
            case "RESERVADO"           -> EstadoAsignacionLote.DESADJUDICADO;
            default -> {
                try {
                    yield EstadoAsignacionLote.valueOf(dbData);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}
