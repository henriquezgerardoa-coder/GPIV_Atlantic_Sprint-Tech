package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SolicitudResolverCambioRubro(
    @NotNull(message = "Debe indicar si se aprueba o rechaza")
    Boolean aprobada,

    @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
    String motivoRechazo,

    @Size(max = 120, message = "El nombre del nuevo rubro no puede superar 120 caracteres")
    String nombreNuevoRubro,

    @NotNull(message = "El puntaje de impacto operativo es obligatorio")
    @Min(value = 1, message = "El puntaje de impacto operativo debe estar entre 1 y 5")
    @Max(value = 5, message = "El puntaje de impacto operativo debe estar entre 1 y 5")
    Integer puntajeImpactoOperativo,

    @NotNull(message = "El puntaje de compatibilidad del parque es obligatorio")
    @Min(value = 1, message = "El puntaje de compatibilidad del parque debe estar entre 1 y 5")
    @Max(value = 5, message = "El puntaje de compatibilidad del parque debe estar entre 1 y 5")
    Integer puntajeCompatibilidadParque,

    @NotNull(message = "El puntaje de viabilidad tecnica es obligatorio")
    @Min(value = 1, message = "El puntaje de viabilidad tecnica debe estar entre 1 y 5")
    @Max(value = 5, message = "El puntaje de viabilidad tecnica debe estar entre 1 y 5")
    Integer puntajeViabilidadTecnica,

    @NotNull(message = "El puntaje de cumplimiento normativo es obligatorio")
    @Min(value = 1, message = "El puntaje de cumplimiento normativo debe estar entre 1 y 5")
    @Max(value = 5, message = "El puntaje de cumplimiento normativo debe estar entre 1 y 5")
    Integer puntajeCumplimientoNormativo,

    @Size(max = 1000, message = "Las observaciones de la rubrica no pueden superar 1000 caracteres")
    String observacionesRubrica
) {
}
