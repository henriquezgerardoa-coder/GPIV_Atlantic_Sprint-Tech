package com.gpiv.atlanticsprinttech.commons.radicacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudRelevamientoPedidoLotes(
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener formato valido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    String correo,
    @NotBlank(message = "La razon social es obligatoria")
    @Size(max = 180, message = "La razon social no puede superar 180 caracteres")
    String razonSocialEmpresa,
    @NotBlank(message = "El CUIT es obligatorio")
    @Pattern(regexp = "^[0-9-]{11,13}$", message = "El CUIT solo puede contener digitos y guiones")
    String cuit,
    @NotBlank(message = "Ingresos brutos es obligatorio")
    @Size(max = 80, message = "Ingresos brutos no puede superar 80 caracteres")
    String ingresosBrutos,
    @NotBlank(message = "La actividad principal es obligatoria")
    @Size(max = 200, message = "La actividad principal no puede superar 200 caracteres")
    String actividadPrincipal,
    @Size(max = 200, message = "La actividad secundaria no puede superar 200 caracteres")
    String actividadSecundaria,
    @NotBlank(message = "El tipo de empresa es obligatorio")
    @Pattern(regexp = "NUEVA|EXISTENTE", message = "El tipo de empresa debe ser NUEVA o EXISTENTE")
    String tipoEmpresa,
    @Size(max = 120, message = "El objeto del proyecto no puede superar 120 caracteres")
    String objetoProyecto,
    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 250, message = "La direccion no puede superar 250 caracteres")
    String direccion,
    @NotBlank(message = "La persona referente es obligatoria")
    @Size(max = 150, message = "La persona referente no puede superar 150 caracteres")
    String personaReferente,
    @NotBlank(message = "El telefono es obligatorio")
    @Size(max = 40, message = "El telefono no puede superar 40 caracteres")
    String telefono,
    @NotBlank(message = "El correo electronico de contacto es obligatorio")
    @Email(message = "El correo electronico debe tener formato valido")
    @Size(max = 150, message = "El correo electronico no puede superar 150 caracteres")
    String correoElectronico,
    @NotBlank(message = "El rubro es obligatorio")
    @Pattern(regexp = "SERVICIOS|BIENES|BIENES_Y_SERVICIOS|OTROS", message = "El rubro enviado no es valido")
    String rubro,
    @Size(max = 120, message = "El detalle de rubro otros no puede superar 120 caracteres")
    String rubroOtro,
    @NotBlank(message = "La descripcion del servicio o bien ofrecido es obligatoria")
    @Size(max = 800, message = "La descripcion no puede superar 800 caracteres")
    String descripcionServicioBienOfrecido,
    @NotBlank(message = "El emplazamiento actual es obligatorio")
    @Pattern(regexp = "PROPIO|ALQUILADO", message = "El emplazamiento actual debe ser PROPIO o ALQUILADO")
    String emplazamientoActual,
    @NotNull(message = "El personal jerarquico es obligatorio")
    @Min(value = 0, message = "El personal jerarquico no puede ser negativo")
    Integer personalJerarquico,
    @NotNull(message = "El personal de produccion es obligatorio")
    @Min(value = 0, message = "El personal de produccion no puede ser negativo")
    Integer personalProduccion,
    @NotNull(message = "El personal administrativo es obligatorio")
    @Min(value = 0, message = "El personal administrativo no puede ser negativo")
    Integer personalAdministrativo,
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
    Boolean necesitaSalonCoworking,
    @NotBlank(message = "El plan ambiental es obligatorio")
    @Size(max = 1000, message = "El plan ambiental no puede superar 1000 caracteres")
    String planAmbiental,
    @NotBlank(message = "La rentabilidad estimada es obligatoria")
    @Size(max = 400, message = "La rentabilidad estimada no puede superar 400 caracteres")
    String rentabilidadEstimada
) {
}
