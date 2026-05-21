package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionDocumento;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionHistorial;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioRadicacion;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudRelevamientoPedidoLotes;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoAsignacionLote;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionDocumento;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionHistorial;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.dominio.TipoDocumentoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioRadicacionImpl implements ServicioRadicacion {

    private static final long MAX_TAMANO_ARCHIVO = 10L * 1024L * 1024L;
    private static final long MAX_TAMANO_TOTAL_SOLICITUD = 30L * 1024L * 1024L;
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("pdf", "doc", "docx", "jpg", "jpeg", "png");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<Integer> OPCIONES_TIEMPO_RADICACION = Set.of(6, 12, 24, 36);
    private static final Set<Integer> OPCIONES_NECESIDAD_M2 = Set.of(1200, 1800, 2500, 3300, 5000, 6000);
    private static final String TIPO_SOLICITUD_PEDIDO_LOTES = "PEDIDO_LOTES";
    private static final Map<EstadoRadicacion, Set<EstadoRadicacion>> TRANSICIONES_VALIDAS = Map.of(
        EstadoRadicacion.PENDIENTE, Set.of(EstadoRadicacion.EN_REVISION, EstadoRadicacion.RECHAZADA, EstadoRadicacion.CANCELADA),
        EstadoRadicacion.EN_REVISION, Set.of(
            EstadoRadicacion.APROBADA,
            EstadoRadicacion.RECHAZADA,
            EstadoRadicacion.REQUIERE_INFORMACION_ADICIONAL,
            EstadoRadicacion.CANCELADA
        ),
        EstadoRadicacion.REQUIERE_INFORMACION_ADICIONAL, Set.of(EstadoRadicacion.EN_REVISION, EstadoRadicacion.RECHAZADA, EstadoRadicacion.CANCELADA),
        EstadoRadicacion.APROBADA, Set.of(EstadoRadicacion.RADICADA),
        EstadoRadicacion.RADICADA, Set.of(),
        EstadoRadicacion.RECHAZADA, Set.of(),
        EstadoRadicacion.CANCELADA, Set.of()
    );

    private final RepositorioRadicacionSolicitud repositorioRadicacionSolicitud;
    private final RepositorioRadicacionHistorial repositorioRadicacionHistorial;
    private final RepositorioRadicacionDocumento repositorioRadicacionDocumento;
    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioLote repositorioLote;
    private final ServicioContextoUsuario servicioContextoUsuario;
    private final ServicioAuditLog servicioAuditLog;
    private final ObjectMapper objectMapper;

    public ServicioRadicacionImpl(
        RepositorioRadicacionSolicitud repositorioRadicacionSolicitud,
        RepositorioRadicacionHistorial repositorioRadicacionHistorial,
        RepositorioRadicacionDocumento repositorioRadicacionDocumento,
        RepositorioEmpresa repositorioEmpresa,
        RepositorioLote repositorioLote,
        ServicioContextoUsuario servicioContextoUsuario,
        ServicioAuditLog servicioAuditLog,
        ObjectMapper objectMapper
    ) {
        this.repositorioRadicacionSolicitud = repositorioRadicacionSolicitud;
        this.repositorioRadicacionHistorial = repositorioRadicacionHistorial;
        this.repositorioRadicacionDocumento = repositorioRadicacionDocumento;
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioLote = repositorioLote;
        this.servicioContextoUsuario = servicioContextoUsuario;
        this.servicioAuditLog = servicioAuditLog;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RadicacionSolicitud> listar(String identificadorIngreso, EstadoRadicacion estado, LocalDate desde, LocalDate hasta) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        Long empresaId = null;
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            if (usuario.getEmpresaId() == null) {
                return List.of();
            }
            empresaId = usuario.getEmpresaId();
        }
        return repositorioRadicacionSolicitud.buscarFiltrado(empresaId, estado, desde, hasta);
    }

    @Override
    public RadicacionSolicitud obtenerPorId(String identificadorIngreso, Long id) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
            return repositorioRadicacionSolicitud.findByIdAndEmpresaIdConEmpresa(id, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Radicacion no encontrada"));
        }
        return repositorioRadicacionSolicitud.findByIdConEmpresa(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Radicacion no encontrada"));
    }

    @Override
    public RadicacionSolicitud crear(
        String identificadorIngreso,
        String tipoSolicitud,
        String descripcion,
        String usoEstimativo,
        SolicitudRelevamientoPedidoLotes relevamientoPedidoLotes
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
        Empresa empresa = repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa asociada no existe"));

        String numero = generarNumeroRadicado();
        String tipoSolicitudNormalizado = tipoSolicitud.trim();
        String usoEstimativoNormalizado = usoEstimativo == null ? null : usoEstimativo.trim();
        if (usoEstimativoNormalizado != null && usoEstimativoNormalizado.isBlank()) {
            usoEstimativoNormalizado = null;
        }
        validarConsistenciaRelevamiento(tipoSolicitudNormalizado, relevamientoPedidoLotes);
        String relevamientoPedidoLotesJson = serializarRelevamiento(relevamientoPedidoLotes);
        RadicacionSolicitud nueva = RadicacionSolicitud.crear(
            numero,
            empresa,
            tipoSolicitudNormalizado,
            descripcion.trim(),
            usoEstimativoNormalizado,
            relevamientoPedidoLotesJson
        );
        RadicacionSolicitud guardada = repositorioRadicacionSolicitud.save(nueva);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(guardada, guardada.getEstado(), "Solicitud creada", identificadorIngreso));
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "CREACION", "RadicacionSolicitud",
            guardada.getNumeroRadicado(),
            null,
            guardada.getTipoSolicitud() + " | " + guardada.getEstado().name(),
            obtenerIpActual()
        );
        return guardada;
    }

    private void validarConsistenciaRelevamiento(String tipoSolicitud, SolicitudRelevamientoPedidoLotes relevamiento) {
        boolean esPedidoLotes = TIPO_SOLICITUD_PEDIDO_LOTES.equalsIgnoreCase(tipoSolicitud);
        if (esPedidoLotes && relevamiento == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar el relevamiento para solicitudes PEDIDO_LOTES");
        }
        if (!esPedidoLotes && relevamiento != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El relevamiento solo aplica para solicitudes PEDIDO_LOTES");
        }
        if (relevamiento == null) {
            return;
        }

        String cuitDigitos = relevamiento.cuit().replace("-", "").trim();
        if (!cuitDigitos.matches("\\d{11}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El CUIT debe contener 11 digitos");
        }

        if (!OPCIONES_TIEMPO_RADICACION.contains(relevamiento.tiempoRadicacionMeses())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tiempo de radicacion debe ser 6, 12, 24 o 36 meses");
        }

        if (!OPCIONES_NECESIDAD_M2.contains(relevamiento.necesidadMetrosCuadrados())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La necesidad de m2 debe coincidir con las opciones permitidas");
        }

        if ("EXISTENTE".equalsIgnoreCase(relevamiento.tipoEmpresa())
            && (relevamiento.objetoProyecto() == null || relevamiento.objetoProyecto().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el objeto del proyecto para empresa existente");
        }

        if ("OTROS".equalsIgnoreCase(relevamiento.rubro())
            && (relevamiento.rubroOtro() == null || relevamiento.rubroOtro().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe detallar el rubro cuando se selecciona OTROS");
        }
    }

    private String serializarRelevamiento(SolicitudRelevamientoPedidoLotes relevamiento) {
        if (relevamiento == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(relevamiento);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo serializar el relevamiento");
        }
    }

    @Override
    public RadicacionSolicitud cambiarEstado(
        String identificadorIngreso,
        Long id,
        EstadoRadicacion estado,
        String comentario,
        Integer tiempoEstimadoObraMeses,
        LocalDate fechaPlazo
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol EMPRESA no puede cambiar el estado");
        }

        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, id);
        validarTransicionEstado(radicacion.getEstado(), estado, comentario);
        if (estado == EstadoRadicacion.APROBADA && fechaPlazo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar la fecha de plazo al aprobar una solicitud");
        }
        EstadoRadicacion estadoAnterior = radicacion.getEstado();
        radicacion.cambiarEstado(estado);
        radicacion.establecerDatosPlazo(tiempoEstimadoObraMeses, fechaPlazo);
        RadicacionSolicitud actualizada = repositorioRadicacionSolicitud.save(radicacion);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(actualizada, estado, comentario, identificadorIngreso));
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "CAMBIO_ESTADO", "RadicacionSolicitud",
            actualizada.getNumeroRadicado(),
            estadoAnterior.name(),
            estado.name(),
            obtenerIpActual()
        );
        return actualizada;
    }

    private void validarTransicionEstado(EstadoRadicacion actual, EstadoRadicacion siguiente, String comentario) {
        if (actual == siguiente) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La radicacion ya se encuentra en ese estado");
        }
        Set<EstadoRadicacion> estadosPermitidos = TRANSICIONES_VALIDAS.getOrDefault(actual, Set.of());
        if (!estadosPermitidos.contains(siguiente)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transicion de estado no permitida");
        }

        boolean requiereComentario = siguiente == EstadoRadicacion.RECHAZADA || siguiente == EstadoRadicacion.CANCELADA;
        if (requiereComentario && (comentario == null || comentario.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar un comentario para el estado seleccionado");
        }
    }

    @Override
    public void registrarObservacion(String identificadorIngreso, Long id, String comentario) {
        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, id);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(radicacion, radicacion.getEstado(), comentario, identificadorIngreso));
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "OBSERVACION", "RadicacionSolicitud",
            radicacion.getNumeroRadicado(),
            null,
            comentario,
            obtenerIpActual()
        );
    }

    @Override
    public RadicacionDocumento adjuntarDocumento(
        String identificadorIngreso,
        Long id,
        TipoDocumentoRadicacion tipoDocumento,
        String descripcion,
        String nombreArchivo,
        String mimeType,
        byte[] contenido
    ) {
        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, id);
        validarArchivo(id, nombreArchivo, mimeType, contenido);

        RadicacionDocumento documento = RadicacionDocumento.crear(
            radicacion,
            tipoDocumento,
            nombreArchivo,
            mimeType == null ? "application/octet-stream" : mimeType,
            contenido.length,
            descripcion,
            identificadorIngreso,
            contenido
        );
        RadicacionDocumento guardado = repositorioRadicacionDocumento.save(documento);
        repositorioRadicacionHistorial.save(
            RadicacionHistorial.crear(
                radicacion,
                radicacion.getEstado(),
                "Documento cargado: " + nombreArchivo,
                identificadorIngreso
            )
        );
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "ADJUNTO_DOCUMENTO", "RadicacionDocumento",
            radicacion.getNumeroRadicado(),
            null,
            nombreArchivo + " (" + tipoDocumento.name() + ")",
            obtenerIpActual()
        );
        return guardado;
    }

    @Override
    public List<RadicacionDocumento> listarDocumentos(String identificadorIngreso, Long id) {
        obtenerPorId(identificadorIngreso, id);
        return repositorioRadicacionDocumento.findByRadicacionIdOrderByFechaSubidaDesc(id);
    }

    @Override
    public RadicacionDocumento obtenerDocumento(String identificadorIngreso, Long radicacionId, Long docId) {
        obtenerPorId(identificadorIngreso, radicacionId);
        return repositorioRadicacionDocumento.findByIdAndRadicacionId(docId, radicacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
    }

    @Override
    public List<RadicacionHistorial> listarHistorial(String identificadorIngreso, Long id) {
        obtenerPorId(identificadorIngreso, id);
        return repositorioRadicacionHistorial.findByRadicacionIdOrderByFechaEventoDesc(id);
    }

    @Override
    public RadicacionSolicitud asignarLote(String identificadorIngreso, Long radicacionId, Long loteId) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol EMPRESA no puede asignar lotes");
        }
        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, radicacionId);
        EstadoRadicacion estadoActual = radicacion.getEstado();
        if (estadoActual == EstadoRadicacion.RECHAZADA || estadoActual == EstadoRadicacion.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede asignar un lote a una solicitud rechazada o cancelada");
        }
        Lote lote = repositorioLote.findByIdConEmpresa(loteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));
        String codigoAnterior = radicacion.getLote() != null ? radicacion.getLote().getCodigo() : null;
        lote.actualizarDatos(lote.getCodigo(), lote.getSuperficieMetrosCuadrados(), true, radicacion.getEmpresa(), lote.getZona());
        lote.actualizarAsignacion(EstadoAsignacionLote.PREADJUDICADO, radicacion.getNumeroRadicado());
        repositorioLote.save(lote);
        radicacion.asignarLote(lote);
        RadicacionSolicitud actualizada = repositorioRadicacionSolicitud.save(radicacion);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(
            actualizada, actualizada.getEstado(),
            "Lote asignado: " + lote.getCodigo(), identificadorIngreso
        ));
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "ASIGNACION_LOTE", "RadicacionSolicitud",
            actualizada.getNumeroRadicado(),
            codigoAnterior,
            lote.getCodigo(),
            obtenerIpActual()
        );
        return actualizada;
    }

    private String generarNumeroRadicado() {
        String prefijo = "RAD-" + LocalDate.now().format(FORMATO_FECHA) + "-";
        String candidato;
        do {
            String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
            candidato = prefijo + sufijo;
        } while (repositorioRadicacionSolicitud.existsByNumeroRadicado(candidato));
        return candidato;
    }

    private String obtenerIpActual() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "desconocida";
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
            ? forwarded.split(",")[0].trim()
            : request.getRemoteAddr();
    }

    private void validarArchivo(Long radicacionId, String nombreArchivo, String mimeType, byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe adjuntar un archivo");
        }
        if (contenido.length > MAX_TAMANO_ARCHIVO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el maximo de 10MB");
        }
        long acumulado = repositorioRadicacionDocumento.totalTamanoPorRadicacion(radicacionId);
        if (acumulado + contenido.length > MAX_TAMANO_TOTAL_SOLICITUD) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "El total de archivos por solicitud supera el maximo permitido");
        }
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo invalido");
        }

        String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de archivo no permitido");
        }

        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (!(mime.contains("pdf") || mime.contains("word") || mime.contains("image") || mime.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo MIME no permitido");
        }

        // Escaneo basico: bloquea payloads con patrones de script embebido.
        String muestra = new String(contenido, 0, Math.min(contenido.length, 2048));
        String muestraNormalizada = muestra.toLowerCase(Locale.ROOT);
        if (muestraNormalizada.contains("<script") || muestraNormalizada.contains("javascript:")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo rechazado por validacion de seguridad");
        }
    }
}
