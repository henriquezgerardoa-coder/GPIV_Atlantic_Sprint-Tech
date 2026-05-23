package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioConversacionMensajeria;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioMensajeMensajeria;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.ParConversacion;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioMensajeria;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.ConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.MensajeMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioMensajeriaImpl implements ServicioMensajeria {

    private final RepositorioConversacionMensajeria repositorioConversacion;
    private final RepositorioMensajeMensajeria repositorioMensaje;
    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioEmpresa repositorioEmpresa;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioMensajeriaImpl(
        RepositorioConversacionMensajeria repositorioConversacion,
        RepositorioMensajeMensajeria repositorioMensaje,
        RepositorioUsuario repositorioUsuario,
        RepositorioEmpresa repositorioEmpresa,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioConversacion = repositorioConversacion;
        this.repositorioMensaje = repositorioMensaje;
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioEmpresa = repositorioEmpresa;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    public List<ConversacionMensajeria> listar(String identificadorIngreso) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = usuario.getEmpresaId();
            if (empresaId == null) {
                return List.of();
            }
            return repositorioConversacion.findByEmpresaIdConDetalleOrderByFechaUltimaActualizacionDesc(empresaId);
        }
        return repositorioConversacion.findAllConDetalleOrderByFechaUltimaActualizacionDesc();
    }

    @Override
    public List<ParConversacion> listarConConMensajes(String identificadorIngreso) {
        return listar(identificadorIngreso).stream()
            .map(c -> new ParConversacion(c, repositorioMensaje.findByConversacionIdConEmisorOrderByFechaEnvioAsc(c.getId())))
            .toList();
    }

    @Override
    public ConversacionMensajeria obtenerPorId(String identificadorIngreso, Long id) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        return obtenerConversacionAccesible(usuario, id);
    }

    @Override
    public List<MensajeMensajeria> listarMensajes(String identificadorIngreso, Long conversacionId) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        obtenerConversacionAccesible(usuario, conversacionId);
        return repositorioMensaje.findByConversacionIdConEmisorOrderByFechaEnvioAsc(conversacionId);
    }

    @Override
    public ConversacionMensajeria crear(String identificadorIngreso, Long usuarioResponsableId, String asunto, String mensaje) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (!servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo una empresa puede iniciar una conversacion");
        }
        Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
        Empresa empresa = repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        Usuario responsable;
        if (usuarioResponsableId == null) {
            responsable = obtenerResponsablePorDefecto();
        } else {
            responsable = repositorioUsuario.findById(usuarioResponsableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinatario no encontrado"));
            if (!esGestor(responsable)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El destinatario debe ser ADMINISTRADOR o DIRECTIVO");
            }
        }

        String asuntoNormalizado = normalizarRequerido(asunto, "El asunto es obligatorio");
        String mensajeNormalizado = normalizarRequerido(mensaje, "El mensaje es obligatorio");

        ConversacionMensajeria conversacion = repositorioConversacion.save(ConversacionMensajeria.crear(empresa, responsable, asuntoNormalizado));
        repositorioMensaje.save(MensajeMensajeria.crear(conversacion, usuario, mensajeNormalizado));
        conversacion.registrarActividad();
        return repositorioConversacion.save(conversacion);
    }

    @Override
    public ConversacionMensajeria responder(String identificadorIngreso, Long conversacionId, String mensaje) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        ConversacionMensajeria conversacion = obtenerConversacionAccesible(usuario, conversacionId);
        String mensajeNormalizado = normalizarRequerido(mensaje, "El mensaje es obligatorio");
        repositorioMensaje.save(MensajeMensajeria.crear(conversacion, usuario, mensajeNormalizado));
        conversacion.registrarActividad();
        return repositorioConversacion.save(conversacion);
    }

    @Override
    public List<Usuario> listarDestinatarios(String identificadorIngreso) {
        servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        return repositorioUsuario.findActivosConRolesGestion(Set.of(RolUsuario.ADMINISTRADOR, RolUsuario.DIRECTIVO));
    }

    @Override
    public ConversacionMensajeria crearConsultaPublica(
        String nombreOEmpresa,
        String correoElectronico,
        String telefono,
        String asunto,
        String mensaje
    ) {
        String nombreNormalizado = normalizarRequerido(nombreOEmpresa, "Debe ingresar su nombre o empresa");
        String correoNormalizado = normalizarRequerido(correoElectronico, "El correo electronico es obligatorio");
        String asuntoNormalizado = normalizarRequerido(asunto, "El asunto es obligatorio");
        String mensajeNormalizado = normalizarRequerido(mensaje, "El mensaje es obligatorio");
        String telefonoNormalizado = telefono == null ? null : telefono.trim();
        if (telefonoNormalizado != null && telefonoNormalizado.isBlank()) {
            telefonoNormalizado = null;
        }

        Usuario responsable = obtenerResponsablePorDefecto();
        ConversacionMensajeria conversacion = repositorioConversacion.save(
            ConversacionMensajeria.crearConsultaPublica(
                responsable,
                asuntoNormalizado,
                nombreNormalizado,
                correoNormalizado,
                telefonoNormalizado
            )
        );
        repositorioMensaje.save(MensajeMensajeria.crearExterno(conversacion, nombreNormalizado, mensajeNormalizado));
        conversacion.registrarActividad();
        return repositorioConversacion.save(conversacion);
    }

    private ConversacionMensajeria obtenerConversacionAccesible(Usuario usuario, Long conversacionId) {
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            return repositorioConversacion.findByIdAndEmpresaIdConDetalle(conversacionId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversacion no encontrada"));
        }
        if (esGestor(usuario)) {
            return repositorioConversacion.findByIdConDetalle(conversacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversacion no encontrada"));
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta conversacion");
    }

    private boolean esGestor(Usuario usuario) {
        return usuario.tieneRol(RolUsuario.ADMINISTRADOR) || usuario.tieneRol(RolUsuario.DIRECTIVO);
    }

    private Usuario obtenerResponsablePorDefecto() {
        List<Usuario> gestores = repositorioUsuario.findActivosConRolesGestion(Set.of(RolUsuario.ADMINISTRADOR, RolUsuario.DIRECTIVO));
        return gestores.stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No hay usuarios ADMINISTRADOR/DIRECTIVO disponibles para recibir consultas"));
    }

    private String normalizarRequerido(String valor, String mensajeError) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeError);
        }
        return valor.trim();
    }
}

