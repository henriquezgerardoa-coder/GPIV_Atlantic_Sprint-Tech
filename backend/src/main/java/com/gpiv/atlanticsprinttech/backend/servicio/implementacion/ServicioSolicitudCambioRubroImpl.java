package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRubro;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioSolicitudCambioRubro;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioMensajeria;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioSolicitudCambioRubro;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoCambioRubro;
import com.gpiv.atlanticsprinttech.entities.dominio.Rubro;
import com.gpiv.atlanticsprinttech.entities.dominio.SolicitudCambioRubro;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioSolicitudCambioRubroImpl implements ServicioSolicitudCambioRubro {

    private final RepositorioSolicitudCambioRubro repositorioSolicitud;
    private final RepositorioRubro repositorioRubro;
    private final RepositorioEmpresa repositorioEmpresa;
    private final ServicioMensajeria servicioMensajeria;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioSolicitudCambioRubroImpl(
        RepositorioSolicitudCambioRubro repositorioSolicitud,
        RepositorioRubro repositorioRubro,
        RepositorioEmpresa repositorioEmpresa,
        ServicioMensajeria servicioMensajeria,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioSolicitud = repositorioSolicitud;
        this.repositorioRubro = repositorioRubro;
        this.repositorioEmpresa = repositorioEmpresa;
        this.servicioMensajeria = servicioMensajeria;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    public List<Rubro> listarRubros() {
        return repositorioRubro.findAllOrdenados();
    }

    @Override
    public List<SolicitudCambioRubro> listar(String identificadorIngreso) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            return repositorioSolicitud.findByEmpresaIdConDetalle(empresaId);
        }
        return repositorioSolicitud.findAllConDetalle();
    }

    @Override
    public SolicitudCambioRubro crear(String identificadorIngreso, Long rubroSolicitadoId, String justificacion, String descripcionOtros) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (!servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo el rol EMPRESA puede solicitar cambio de rubro");
        }
        Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);

        if (repositorioSolicitud.existsByEmpresaIdAndEstado(empresaId, EstadoCambioRubro.PENDIENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya existe una solicitud de cambio de rubro pendiente para esta empresa");
        }

        Empresa empresa = repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        Rubro rubroSolicitado = repositorioRubro.findById(rubroSolicitadoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rubro no encontrado"));

        Rubro rubroActual = empresa.getRubro();
        if (rubroActual != null && rubroActual.getId().equals(rubroSolicitadoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El rubro solicitado es el mismo que el rubro actual");
        }

        if ("Otros".equalsIgnoreCase(rubroSolicitado.getNombre())
                && (descripcionOtros == null || descripcionOtros.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Debe especificar cual es el rubro cuando selecciona 'Otros'");
        }

        String rubroAnteriorNombre = rubroActual != null ? rubroActual.getNombre() : null;
        SolicitudCambioRubro solicitud = SolicitudCambioRubro.crear(
            empresa, rubroSolicitado, rubroAnteriorNombre, justificacion.trim(),
            descripcionOtros != null ? descripcionOtros.trim() : null, identificadorIngreso
        );
        return repositorioSolicitud.save(solicitud);
    }

    @Override
    public SolicitudCambioRubro resolver(
        String identificadorIngreso,
        Long solicitudId,
        boolean aprobada,
        String motivoRechazo,
        String nombreNuevoRubro
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo ADMINISTRADOR o DIRECTIVO pueden resolver solicitudes de cambio de rubro");
        }

        SolicitudCambioRubro solicitud = repositorioSolicitud.findByIdConDetalle(solicitudId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        solicitud.validarResoluble();

        if (aprobada) {
            solicitud.aprobar(identificadorIngreso);
            Empresa empresa = solicitud.getEmpresa();
            Rubro rubroFinal = solicitud.getRubroSolicitado();
            if (solicitud.esRubroOtros() && nombreNuevoRubro != null && !nombreNuevoRubro.isBlank()) {
                rubroFinal = repositorioRubro.save(
                    Rubro.crear(nombreNuevoRubro.trim(), solicitud.getDescripcionOtros(), false)
                );
            }
            empresa.asignarRubro(rubroFinal);
            repositorioEmpresa.save(empresa);
        } else {
            if (motivoRechazo == null || motivoRechazo.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El motivo de rechazo es obligatorio al rechazar una solicitud");
            }
            String motivoNormalizado = motivoRechazo.trim();
            solicitud.rechazar(identificadorIngreso, motivoNormalizado);
            servicioMensajeria.notificarEmpresa(
                solicitud.getEmpresa().getId(),
                "Solicitud de cambio de rubro rechazada",
                "Tu solicitud de cambio de rubro para la empresa " + solicitud.getEmpresa().getNombre()
                    + " fue rechazada. Motivo: " + motivoNormalizado
            );
        }

        return repositorioSolicitud.save(solicitud);
    }
}
