package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpresa;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Rubro;
import org.springframework.stereotype.Component;

@Component
public class MapeadorEmpresa {

    public Empresa desdeSolicitud(SolicitudEmpresa solicitud) {
        return Empresa.crear(
            solicitud.nombre(),
            solicitud.razonSocial(),
            solicitud.cuit(),
            solicitud.direccion(),
            solicitud.actividadEconomica(),
            solicitud.correoElectronico(),
            solicitud.telefono()
        );
    }

    public RespuestaEmpresa toRespuesta(Empresa empresa, boolean permiteServiciosPostRadicacion) {
        Rubro rubro = empresa.getRubro();
        return new RespuestaEmpresa(
            empresa.getId(),
            empresa.getNombre(),
            empresa.getRazonSocial(),
            empresa.getCuit(),
            empresa.getDireccion(),
            empresa.getActividadEconomica(),
            empresa.getCorreoElectronico(),
            empresa.getTelefono(),
            empresa.getCantidadEmpleados(),
            permiteServiciosPostRadicacion,
            rubro != null ? rubro.getId() : null,
            rubro != null ? rubro.getNombre() : null
        );
    }
}
