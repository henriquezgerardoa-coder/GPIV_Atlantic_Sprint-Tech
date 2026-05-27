package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioConversacionMensajeria;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioMensajeMensajeria;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.mapeador.ParConversacion;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioMensajeria;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.ConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.MensajeMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioMensajeriaImpl implements ServicioMensajeria {

    private static final Set<RolUsuario> ROLES_GESTION = EnumSet.of(
        RolUsuario.ADMINISTRADOR, RolUsuario.DIRECTIVO, RolUsuario.SECRETARIO, RolUsuario.TECNICO);

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
    @Transactional(readOnly = true)
    public List<ConversacionMensajeria> listar(String identificadorIngreso) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);

        List<ConversacionMensajeria> porParticipante = repositorioConversacion
            .findByParticipanteConDetalleOrderByFechaUltimaActualizacionDesc(usuario.getId());

        if (!servicioContextoUsuario.esRolEmpresa(usuario)) {
            return porParticipante;
        }

        Long empresaId = usuario.getEmpresaId();
        if (empresaId == null) return porParticipante;

        List<ConversacionMensajeria> porEmpresa = repositorioConversacion
            .findByEmpresaIdConDetalleOrderByFechaUltimaActualizacionDesc(empresaId);

        Map<Long, ConversacionMensajeria> merged = new LinkedHashMap<>();
        Stream.concat(porParticipante.stream(), porEmpresa.stream())
            .forEach(c -> merged.put(c.getId(), c));
        return merged.values().stream()
            .sorted(Comparator.comparing(ConversacionMensajeria::getFechaUltimaActualizacion).reversed())
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParConversacion> listarConConMensajes(String identificadorIngreso) {
        List<ConversacionMensajeria> conversaciones = listar(identificadorIngreso);
        if (conversaciones.isEmpty()) return List.of();
        List<Long> ids = conversaciones.stream().map(ConversacionMensajeria::getId).toList();
        Map<Long, List<MensajeMensajeria>> porConversacion = repositorioMensaje
            .findByConversacionIdsConEmisor(ids).stream()
            .collect(Collectors.groupingBy(m -> m.getConversacion().getId()));
        return conversaciones.stream()
            .map(c -> new ParConversacion(c, porConversacion.getOrDefault(c.getId(), List.of())))
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

        String asuntoNormalizado = normalizarRequerido(asunto, "El asunto es obligatorio");
        String mensajeNormalizado = normalizarRequerido(mensaje, "El mensaje es obligatorio");

        Usuario responsable;
        if (usuarioResponsableId != null) {
            responsable = repositorioUsuario.findById(usuarioResponsableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinatario no encontrado"));
            if (!responsable.isActivo()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El destinatario no esta activo");
            }
        } else {
            responsable = obtenerResponsablePorDefecto();
        }

        ConversacionMensajeria conversacion;
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            Empresa empresa = repositorioEmpresa.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
            conversacion = repositorioConversacion.save(ConversacionMensajeria.crear(empresa, responsable, asuntoNormalizado));
        } else {
            conversacion = repositorioConversacion.save(ConversacionMensajeria.crearEntreUsuarios(usuario, responsable, asuntoNormalizado));
        }

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
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        return repositorioUsuario.findByActivoTrueOrderByNombreCompletoAsc().stream()
            .filter(u -> !u.getId().equals(usuario.getId()))
            .toList();
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

    @Override
    public ConversacionMensajeria notificarEmpresa(Long empresaId, String asunto, String mensaje) {
        Empresa empresa = repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
        String asuntoNormalizado = normalizarRequerido(asunto, "El asunto es obligatorio");
        String mensajeNormalizado = normalizarRequerido(mensaje, "El mensaje es obligatorio");

        Usuario responsable = obtenerResponsablePorDefecto();
        ConversacionMensajeria conversacion = repositorioConversacion.save(
            ConversacionMensajeria.crear(empresa, responsable, asuntoNormalizado)
        );
        repositorioMensaje.save(
            MensajeMensajeria.crearExterno(conversacion, "Sistema GPIV", mensajeNormalizado)
        );
        conversacion.registrarActividad();
        return repositorioConversacion.save(conversacion);
    }

    private ConversacionMensajeria obtenerConversacionAccesible(Usuario usuario, Long conversacionId) {
        ConversacionMensajeria conversacion = repositorioConversacion.findByIdConDetalle(conversacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversacion no encontrada"));
        if (esParticipante(usuario, conversacion)) {
            return conversacion;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta conversacion");
    }

    private boolean esParticipante(Usuario usuario, ConversacionMensajeria conversacion) {
        Long uid = usuario.getId();
        if (uid.equals(conversacion.getUsuarioResponsableId())) return true;
        if (conversacion.getUsuarioIniciadorId() != null && uid.equals(conversacion.getUsuarioIniciadorId())) return true;
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = usuario.getEmpresaId();
            return empresaId != null && empresaId.equals(conversacion.getEmpresaId());
        }
        return false;
    }


    private Usuario obtenerResponsablePorDefecto() {
        List<Usuario> gestores = repositorioUsuario.findActivosConRolesGestion(ROLES_GESTION);
        return gestores.stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No hay usuarios gestores disponibles para recibir consultas"));
    }

    private String normalizarRequerido(String valor, String mensajeError) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeError);
        }
        return valor.trim();
    }
}
