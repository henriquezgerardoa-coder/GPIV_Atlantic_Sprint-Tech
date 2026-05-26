package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.configuracion.PropiedadesRegistroPublico;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioCorreoVerificacion;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.RegistroIntentosEnMemoria;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.backend.util.UtilRed;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioUsuarioImpl implements ServicioUsuario {
    private static final Pattern PATRON_CLAVE_SEGURA = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$");

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioEmpresa repositorioEmpresa;
    private final PasswordEncoder codificadorClave;
    private final ServicioCorreoVerificacion servicioCorreoVerificacion;
    private final PropiedadesRegistroPublico propiedadesRegistroPublico;
    private final RegistroIntentosEnMemoria registroIntentosEnMemoria;
    private final ServicioAuditLog servicioAuditLog;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioUsuarioImpl(
        RepositorioUsuario repositorioUsuario,
        RepositorioEmpresa repositorioEmpresa,
        PasswordEncoder codificadorClave,
        ServicioCorreoVerificacion servicioCorreoVerificacion,
        PropiedadesRegistroPublico propiedadesRegistroPublico,
        RegistroIntentosEnMemoria registroIntentosEnMemoria,
        ServicioAuditLog servicioAuditLog,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioEmpresa = repositorioEmpresa;
        this.codificadorClave = codificadorClave;
        this.servicioCorreoVerificacion = servicioCorreoVerificacion;
        this.propiedadesRegistroPublico = propiedadesRegistroPublico;
        this.registroIntentosEnMemoria = registroIntentosEnMemoria;
        this.servicioAuditLog = servicioAuditLog;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }
    @Override
    public List<Usuario> listar() {
        return repositorioUsuario.findAll(Sort.by(Sort.Direction.ASC, "nombreCompleto"));
    }
    @Override
    public List<Usuario> listarPorEmpresa(Long empresaId) {
        return repositorioUsuario.findByEmpresa_IdOrderByIdAsc(empresaId);
    }
    @Override
    public Usuario obtenerPorId(Long id) {
        return repositorioUsuario.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
    @Override
    public Usuario obtenerPorIdentificador(String identificador) {
        return obtenerUsuarioPorIdentificador(identificador);
    }
    @Override
    public Usuario crear(String nombreUsuario, String nombreCompleto, String clavePlano, boolean activo, Set<RolUsuario> roles, Long empresaId) {
        Set<RolUsuario> rolesNormalizados = normalizarRoles(roles);
        validarRoles(rolesNormalizados);
        if (repositorioUsuario.existsByNombreUsuario(nombreUsuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre");
        }
        Empresa empresa = resolverEmpresaParaUsuario(rolesNormalizados, empresaId);
        Usuario usuario = Usuario.crear(
            nombreUsuario,
            nombreCompleto,
            codificadorClave.encode(clavePlano),
            activo,
            rolesNormalizados
        );
        String autorizador = servicioContextoUsuario.obtenerIdentificadorActual();
        usuario.actualizarEmpresa(empresa);
        usuario.setAutorizadoPor(autorizador);
        Usuario guardado = repositorioUsuario.save(usuario);
        servicioAuditLog.registrarEvento(
            autorizador, "CREACION_USUARIO", "Usuario",
            guardado.getNombreUsuario(),
            null,
            "roles=" + rolesNormalizados + " | activo=" + activo,
            UtilRed.obtenerIpActual()
        );
        return guardado;
    }
    @Override
    public Usuario crearComoEmpresa(String nombreUsuario, String nombreCompleto, String clavePlano, Long empresaId, String autorizador) {
        validarPoliticaClave(clavePlano);
        if (repositorioUsuario.existsByNombreUsuario(nombreUsuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre");
        }
        Empresa empresa = repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa indicada no existe"));
        Usuario usuario = Usuario.crear(
            nombreUsuario,
            nombreCompleto,
            codificadorClave.encode(clavePlano),
            true,
            EnumSet.of(RolUsuario.EMPRESA)
        );
        usuario.actualizarEmpresa(empresa);
        usuario.setAutorizadoPor(autorizador);
        Usuario guardado = repositorioUsuario.save(usuario);
        servicioAuditLog.registrarEvento(
            autorizador, "CREACION_USUARIO", "Usuario",
            guardado.getNombreUsuario(),
            null,
            "roles=EMPRESA | autorizadoPor=" + autorizador,
            UtilRed.obtenerIpActual()
        );
        return guardado;
    }
    @Override
    public Usuario actualizar(Long id, String nombreCompleto, boolean activo, Set<RolUsuario> roles, Long empresaId) {
        Set<RolUsuario> rolesNormalizados = normalizarRoles(roles);
        validarRoles(rolesNormalizados);
        Usuario usuario = obtenerPorId(id);
        String anteriorEstado = "activo=" + usuario.isActivo() + " | roles=" + usuario.getRoles();
        Empresa empresa = resolverEmpresaParaUsuario(rolesNormalizados, empresaId);
        usuario.actualizarDatos(nombreCompleto, activo, rolesNormalizados, empresa);
        Usuario guardado = repositorioUsuario.save(usuario);
        servicioAuditLog.registrarEvento(
            servicioContextoUsuario.obtenerIdentificadorActual(), "ACTUALIZACION_USUARIO", "Usuario",
            guardado.getNombreUsuario(),
            anteriorEstado,
            "activo=" + activo + " | roles=" + rolesNormalizados,
            UtilRed.obtenerIpActual()
        );
        return guardado;
    }
    @Override
    public void eliminar(Long id) {
        Usuario usuario = obtenerPorId(id);
        String nombreUsuario = usuario.getNombreUsuario();
        String datos = usuario.getNombreCompleto() + " | roles=" + usuario.getRoles();
        repositorioUsuario.delete(usuario);
        servicioAuditLog.registrarEvento(
            servicioContextoUsuario.obtenerIdentificadorActual(), "ELIMINACION_USUARIO", "Usuario",
            nombreUsuario,
            datos,
            null,
            UtilRed.obtenerIpActual()
        );
    }
    @Override
    public void restablecerClave(Long id, String claveNueva) {
        Usuario usuario = obtenerPorId(id);
        usuario.actualizarClaveAccesoHash(codificadorClave.encode(claveNueva));
        repositorioUsuario.save(usuario);
        servicioAuditLog.registrarEvento(
            servicioContextoUsuario.obtenerIdentificadorActual(), "RESTABLECIMIENTO_CLAVE", "Usuario",
            usuario.getNombreUsuario(),
            null, null,
            UtilRed.obtenerIpActual()
        );
    }
    @Override
    public void cambiarClavePropia(String nombreUsuario, String claveActual, String claveNueva) {
        Usuario usuario = repositorioUsuario.findByNombreUsuarioAndActivoTrue(nombreUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!codificadorClave.matches(claveActual, usuario.getClaveAccesoHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave actual es incorrecta");
        }
        usuario.actualizarClaveAccesoHash(codificadorClave.encode(claveNueva));
        repositorioUsuario.save(usuario);
        servicioAuditLog.registrarEvento(
            nombreUsuario, "CAMBIO_CLAVE_PROPIA", "Usuario",
            nombreUsuario,
            null, null,
            UtilRed.obtenerIpActual()
        );
    }

    @Override
    public Usuario actualizarPerfilPropio(String identificadorIngreso, String nombreCompleto, String correoElectronico) {
        Usuario usuario = obtenerUsuarioPorIdentificador(identificadorIngreso);
        String nombreNormalizado = nombreCompleto == null ? "" : nombreCompleto.trim();
        if (nombreNormalizado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre completo es obligatorio");
        }

        String correoNormalizado = normalizarCorreoOpcional(correoElectronico);
        if (correoNormalizado != null
            && !correoNormalizado.equalsIgnoreCase(usuario.getCorreoElectronico())
            && repositorioUsuario.existsByCorreoElectronicoIgnoreCase(correoNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electronico ya esta registrado");
        }

        String anteriorPerfil = usuario.getNombreCompleto() + " | " + usuario.getCorreoElectronico();
        usuario.actualizarPerfilPersonal(nombreNormalizado, correoNormalizado);
        Usuario guardado = repositorioUsuario.save(usuario);
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "ACTUALIZACION_PERFIL", "Usuario",
            guardado.getNombreUsuario(),
            anteriorPerfil,
            nombreNormalizado + " | " + correoNormalizado,
            UtilRed.obtenerIpActual()
        );
        return guardado;
    }

    @Override
    public void registrarPublico(String nombreUsuario, String correoElectronico, String clave, String confirmacionClave, String ipCliente) {
        Duration ventana = obtenerVentanaLimites();
        String claveIntento = "registro:" + ipCliente + ":" + normalizarCorreo(correoElectronico);
        if (registroIntentosEnMemoria.excedeLimite(claveIntento, propiedadesRegistroPublico.getLimites().getMaxIntentosRegistro(), ventana)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos de registro. Intente nuevamente mas tarde");
        }
        registroIntentosEnMemoria.registrarIntento(claveIntento, ventana);

        String correoNormalizado = normalizarCorreo(correoElectronico);
        if (!clave.equals(confirmacionClave)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La confirmacion de contrasena no coincide");
        }
        validarPoliticaClave(clave);
        if (repositorioUsuario.existsByNombreUsuario(nombreUsuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya esta registrado");
        }
        if (repositorioUsuario.existsByCorreoElectronicoIgnoreCase(correoNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electronico ya esta registrado");
        }

        Usuario usuario = Usuario.crearPendienteVerificacion(
            nombreUsuario,
            nombreUsuario,
            correoNormalizado,
            codificadorClave.encode(clave),
            EnumSet.of(RolUsuario.EMPRESA)
        );
        boolean mailHabilitado = propiedadesRegistroPublico.getMail().isHabilitado();
        renovarTokenVerificacion(usuario);
        if (!mailHabilitado) {
            usuario.marcarEmailVerificado(LocalDateTime.now());
        }
        repositorioUsuario.save(usuario);
        if (mailHabilitado) {
            enviarCorreoVerificacion(usuario);
        }
        servicioAuditLog.registrarEvento(
            correoNormalizado, "REGISTRO_PUBLICO", "Usuario",
            nombreUsuario,
            null,
            "EMPRESA | " + (!propiedadesRegistroPublico.getMail().isHabilitado() ? "auto-verificado" : "pendiente verificacion"),
            ipCliente
        );
    }

    @Override
    public void verificarCorreoElectronico(String tokenVerificacion) {
        Usuario usuario = repositorioUsuario.findByTokenVerificacionEmail(tokenVerificacion)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token es invalido o ya fue utilizado"));
        usuario.validarTokenVigente(LocalDateTime.now());
        usuario.marcarEmailVerificado(LocalDateTime.now());
        repositorioUsuario.save(usuario);
        servicioAuditLog.registrarEvento(
            usuario.getNombreUsuario(), "VERIFICACION_EMAIL", "Usuario",
            usuario.getNombreUsuario(),
            "email_no_verificado",
            "email_verificado",
            UtilRed.obtenerIpActual()
        );
    }

    @Override
    public void reenviarCorreoVerificacion(String correoElectronico, String ipCliente) {
        String correoNormalizado = normalizarCorreo(correoElectronico);
        Duration ventana = obtenerVentanaLimites();
        String claveIntento = "reenvio:" + ipCliente + ":" + correoNormalizado;
        if (registroIntentosEnMemoria.excedeLimite(claveIntento, propiedadesRegistroPublico.getLimites().getMaxReenviosVerificacion(), ventana)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados reenvios. Intente nuevamente mas tarde");
        }
        registroIntentosEnMemoria.registrarIntento(claveIntento, ventana);

        Usuario usuario = repositorioUsuario.findByCorreoElectronicoIgnoreCase(correoNormalizado)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una cuenta asociada a ese correo"));
        usuario.validarCorreoNoVerificado();
        renovarTokenVerificacion(usuario);
        repositorioUsuario.save(usuario);
        enviarCorreoVerificacion(usuario);
    }

    private void validarRoles(Set<RolUsuario> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe asignar al menos un rol");
        }
    }

    private Set<RolUsuario> normalizarRoles(Set<RolUsuario> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return EnumSet.copyOf(roles);
    }

    private Empresa resolverEmpresaParaUsuario(Set<RolUsuario> roles, Long empresaId) {
        boolean esEmpresa = roles.contains(RolUsuario.EMPRESA);
        if (!esEmpresa) {
            return null;
        }
        if (empresaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol EMPRESA requiere una empresa asociada");
        }
        return repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa indicada no existe"));
    }

    private void validarPoliticaClave(String clave) {
        if (clave == null || !PATRON_CLAVE_SEGURA.matcher(clave).matches()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La contrasena debe tener entre 8 y 72 caracteres e incluir mayusculas, minusculas y numeros"
            );
        }
    }

    private String normalizarCorreo(String correoElectronico) {
        if (correoElectronico == null) {
            return "";
        }
        return correoElectronico.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarCorreoOpcional(String correoElectronico) {
        if (correoElectronico == null) {
            return null;
        }
        String normalizado = correoElectronico.trim().toLowerCase(Locale.ROOT);
        return normalizado.isBlank() ? null : normalizado;
    }

    private Usuario obtenerUsuarioPorIdentificador(String identificadorIngreso) {
        String normalizado = normalizarCorreo(identificadorIngreso);
        return repositorioUsuario.findByNombreUsuario(identificadorIngreso)
            .or(() -> repositorioUsuario.findByCorreoElectronicoIgnoreCase(normalizado))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private void renovarTokenVerificacion(Usuario usuario) {
        long horasExpiracion = propiedadesRegistroPublico.getToken().getExpiracionHoras();
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        usuario.iniciarVerificacionEmail(token, LocalDateTime.now().plusHours(horasExpiracion));
    }

    private void enviarCorreoVerificacion(Usuario usuario) {
        String token = usuario.getTokenVerificacionEmail();
        String enlace = propiedadesRegistroPublico.getUrlBaseVerificacion()
            + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        servicioCorreoVerificacion.enviarCorreoVerificacion(
            usuario.getCorreoElectronico(),
            enlace,
            propiedadesRegistroPublico.getToken().getExpiracionHoras(),
            propiedadesRegistroPublico.getContactoSoporte()
        );
    }

    private Duration obtenerVentanaLimites() {
        return Duration.ofMinutes(propiedadesRegistroPublico.getLimites().getVentanaMinutos());
    }
}