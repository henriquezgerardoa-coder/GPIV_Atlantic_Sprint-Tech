package com.gpiv.atlanticsprinttech.commons.comunicacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudRelevamientoPedidoLotes(
    @NotBlank(message = "El tipo de empresa es obligatorio")
    @Pattern(regexp = "NUEVA|EXISTENTE", message = "El tipo de empresa debe ser NUEVA o EXISTENTE")
    String tipoEmpresa,
    @Size(max = 120, message = "El objeto del proyecto no puede superar 120 caracteres")
    String objetoProyecto,
    @NotNull(message = "El tiempo de radicacion es obligatorio")
    Integer tiempoRadicacionMeses,
    @NotNull(message = "La necesidad de m2 es obligatoria")
    Integer necesidadMetrosCuadrados,
    @NotNull(message = "La superficie de trabajo es obligatoria")
    @DecimalMin(value = "0.0", message = "La superficie de trabajo no puede ser negativa")
    Double superficieCubiertaTrabajo,
    @NotNull(message = "La superficie de deposito es obligatoria")
    @DecimalMin(value = "0.0", message = "La superficie de deposito no puede ser negativa")
    Double superficieCubiertaDeposito,
    @NotNull(message = "La superficie para expansion es obligatoria")
    @DecimalMin(value = "0.0", message = "La superficie para expansion no puede ser negativa")
    Double superficieFuturaExpansion,
    @NotNull(message = "La superficie de estacionamiento es obligatoria")
    @DecimalMin(value = "0.0", message = "La superficie de estacionamiento no puede ser negativa")
    Double superficieEstacionamiento,
    @NotNull(message = "Debe indicar si tiene planos")
    Boolean tienePlanos,
    @NotNull(message = "El personal a ocupar es obligatorio")
    @Min(value = 0, message = "El personal a ocupar no puede ser negativo")
    Integer personalAOcupar,
    @NotBlank(message = "Debe indicar materias primas")
    @Size(max = 800, message = "Materias primas no puede superar 800 caracteres")
    String materiasPrimas,
    @NotBlank(message = "Debe indicar destino de la produccion")
    @Size(max = 400, message = "Destino de la produccion no puede superar 400 caracteres")
    String destinoProduccion,
    @NotBlank(message = "La tension de alimentacion es obligatoria")
    @Pattern(regexp = "MEDIA|BAJA", message = "La tension de alimentacion debe ser MEDIA o BAJA")
    String tensionAlimentacion,
    @NotNull(message = "La potencia instalada es obligatoria")
    @DecimalMin(value = "0.0", message = "La potencia instalada no puede ser negativa")
    Double potenciaInstaladaKw,
    @NotNull(message = "El consumo de agua es obligatorio")
    @DecimalMin(value = "0.0", message = "El consumo de agua no puede ser negativo")
    Double aguaLtsMes,
    @NotNull(message = "Debe indicar si necesita gas")
    Boolean requiereGas,
    @NotBlank(message = "Debe indicar tipo de residuos o efluentes")
    @Size(max = 800, message = "El tipo de residuos no puede superar 800 caracteres")
    String tipoResiduosEfluentes,
    @NotNull(message = "Debe indicar si preve tratamiento en planta")
    Boolean tratamientoEnPlanta,
    @NotNull(message = "Debe indicar necesidad de balanza publica")
    Boolean necesitaBalanzaPublica,
    @NotNull(message = "Debe indicar necesidad de comedor unitario")
    Boolean necesitaComedorUnitario,
    @NotNull(message = "Debe indicar necesidad de salon o coworking")
    Boolean necesitaSalonCoworking
) {
}