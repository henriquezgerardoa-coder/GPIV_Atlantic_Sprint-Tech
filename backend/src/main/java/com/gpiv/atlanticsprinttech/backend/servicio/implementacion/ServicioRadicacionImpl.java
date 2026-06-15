package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.backend.evento.RadicacionCreadaEvent;
import com.gpiv.atlanticsprinttech.backend.evento.RadicacionEstadoCambiadoEvent;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioProyectoProductivo;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionDocumento;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionHistorial;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioRadicacion;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import org.springframework.context.ApplicationEventPublisher;
import com.gpiv.atlanticsprinttech.backend.util.UtilRed;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudRelevamientoPedidoLotes;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Lote;
import com.gpiv.atlanticsprinttech.entities.dominio.ProyectoProductivo;
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
import org.springframework.cache.annotation.CacheEvict;
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

    // Firmas binarias (magic bytes) por extensión permitida.
    // Cada extensión puede tener múltiples firmas válidas (el array interno es la secuencia de bytes).
    private static final Map<String, byte[][]> MAGIC_BYTES = Map.of(
        "pdf",  new byte[][]{{0x25, 0x50, 0x44, 0x46}},                            // %PDF
        "png",  new byte[][]{{(byte) 0x89, 0x50, 0x4E, 0x47}},                     // \x89PNG
        "jpg",  new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}},             // JFIF/EXIF
        "jpeg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}},
        "doc",  new byte[][]{{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}},      // OLE2
        "docx", new byte[][]{{0x50, 0x4B, 0x03, 0x04}}                             // ZIP (OOXML)
    );

    private final RepositorioRadicacionSolicitud repositorioRadicacionSolicitud;
    private final RepositorioRadicacionHistorial repositorioRadicacionHistorial;
    private final RepositorioRadicacionDocumento repositorioRadicacionDocumento;
    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioLote repositorioLote;
    private final RepositorioProyectoProductivo repositorioProyecto;
    private final ServicioContextoUsuario servicioContextoUsuario;
    private final ServicioAuditLog servicioAuditLog;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public ServicioRadicacionImpl(
        RepositorioRadicacionSolicitud repositorioRadicacionSolicitud,
        RepositorioRadicacionHistorial repositorioRadicacionHistorial,
        RepositorioRadicacionDocumento repositorioRadicacionDocumento,
        RepositorioEmpresa repositorioEmpresa,
        RepositorioLote repositorioLote,
        RepositorioProyectoProductivo repositorioProyecto,
        ServicioContextoUsuario servicioContextoUsuario,
        ServicioAuditLog servicioAuditLog,
        ApplicationEventPublisher eventPublisher,
        ObjectMapper objectMapper
    ) {
        this.repositorioRadicacionSolicitud = repositorioRadicacionSolicitud;
        this.repositorioRadicacionHistorial = repositorioRadicacionHistorial;
        this.repositorioRadicacionDocumento = repositorioRadicacionDocumento;
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioLote = repositorioLote;
        this.repositorioProyecto = repositorioProyecto;
        this.servicioContextoUsuario = servicioContextoUsuario;
        this.servicioAuditLog = servicioAuditLog;
        this.eventPublisher = eventPublisher;
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
        servicioContextoUsuario.exigirEmpresaActivaParaEscritura(usuario);
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
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(guardada, null, guardada.getEstado(), "Solicitud creada", identificadorIngreso));
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "CREACION", "RadicacionSolicitud",
            guardada.getNumeroRadicado(),
            null,
            guardada.getTipoSolicitud() + " | " + guardada.getEstado().name(),
            UtilRed.obtenerIpActual()
        );
        eventPublisher.publishEvent(new RadicacionCreadaEvent(guardada, identificadorIngreso));
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
    @CacheEvict(value = "lotes", allEntries = true)
    public RadicacionSolicitud cambiarEstado(
        String identificadorIngreso,
        Long id,
        EstadoRadicacion estado,
        String comentario,
        Integer tiempoEstimadoObraMeses,
        LocalDate fechaPlazo,
        LocalDate fechaAprobacion,
        String numeroResolucion
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol EMPRESA no puede cambiar el estado");
        }

        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, id);

        if (estado == EstadoRadicacion.RADICADA) {
            validarRequisitosParaRadicar(radicacion.getId());
        }

        EstadoRadicacion estadoAnterior;
        try {
            estadoAnterior = radicacion.aplicarTransicion(
                estado, comentario, fechaPlazo, tiempoEstimadoObraMeses,
                fechaAprobacion, numeroResolucion, identificadorIngreso
            );
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        Lote lote = radicacion.getLote();
        if (lote != null) {
            if (estado == EstadoRadicacion.APROBADA) {
                lote.ocuparConEmpresa(radicacion.getEmpresa(), radicacion.getNumeroRadicado());
            } else if ((estado == EstadoRadicacion.CANCELADA
                    || estado == EstadoRadicacion.RECHAZADA
                    || estado == EstadoRadicacion.DESADJUDICACION)
                    && lote.isOcupado()) {
                lote.liberarYDisponibilizar();
            }
            repositorioLote.save(lote);
        }
        RadicacionSolicitud actualizada = repositorioRadicacionSolicitud.save(radicacion);

        repositorioRadicacionHistorial.save(
            RadicacionHistorial.crear(actualizada, estadoAnterior, estado, comentario, identificadorIngreso)
        );
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "CAMBIO_ESTADO", "RadicacionSolicitud",
            actualizada.getNumeroRadicado(),
            estadoAnterior.name(),
            estado.name(),
            UtilRed.obtenerIpActual()
        );

        if (estado == EstadoRadicacion.APROBADA && !repositorioProyecto.existsBySolicitudOrigenId(actualizada.getId())) {
            crearProyectoDesdeRadicacion(actualizada, usuario);
        }
        eventPublisher.publishEvent(
            new RadicacionEstadoCambiadoEvent(actualizada, estadoAnterior, estado, comentario, identificadorIngreso)
        );
        return actualizada;
    }

    private void validarRequisitosParaRadicar(Long radicacionId) {
        repositorioProyecto.findBySolicitudOrigenId(radicacionId).ifPresentOrElse(
            p -> {
                if (p.getEstado() != com.gpiv.atlanticsprinttech.entities.dominio.EstadoProyecto.COMPLETADO) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El proyecto productivo asociado debe estar en estado COMPLETADO para radicar el expediente");
                }
            },
            () -> { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No existe proyecto productivo asociado al expediente"); }
        );
    }

    @Override
    public void registrarObservacion(String identificadorIngreso, Long id, String comentario) {
        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, id);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(radicacion, null, radicacion.getEstado(), comentario, identificadorIngreso));
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "OBSERVACION", "RadicacionSolicitud",
            radicacion.getNumeroRadicado(),
            null,
            comentario,
            UtilRed.obtenerIpActual()
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
                null,
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
            UtilRed.obtenerIpActual()
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
        radicacion.validarPermiteAsignarLote();
        Lote lote = repositorioLote.findByIdConEmpresa(loteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));
        if (lote.isOcupado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El lote ya está adjudicado definitivamente y no puede asignarse");
        }
        String codigoAnterior = radicacion.obtenerCodigoLote();
        radicacion.asignarLote(lote);
        RadicacionSolicitud actualizada = repositorioRadicacionSolicitud.save(radicacion);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(
            actualizada, null, actualizada.getEstado(),
            "Lote asignado: " + lote.getCodigo(), identificadorIngreso
        ));
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "ASIGNACION_LOTE", "RadicacionSolicitud",
            actualizada.getNumeroRadicado(),
            codigoAnterior,
            lote.getCodigo(),
            UtilRed.obtenerIpActual()
        );
        return actualizada;
    }

    @Override
    public RadicacionDocumento subirActaRubrica(
        String identificadorIngreso,
        Long radicacionId,
        String nombreArchivo,
        String mimeType,
        byte[] contenido
    ) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (!usuario.tieneRolDeGestion()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el Secretario o Administrador puede cargar el Acta de Rubrica");
        }
        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, radicacionId);
        radicacion.validarPermiteActaRubrica();
        if (mimeType == null || !mimeType.toLowerCase(Locale.ROOT).contains("pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El Acta de Rúbrica debe ser un archivo PDF");
        }
        validarArchivo(radicacionId, nombreArchivo, mimeType, contenido);

        RadicacionDocumento acta = RadicacionDocumento.crear(
            radicacion,
            TipoDocumentoRadicacion.ACTA_RUBRICA,
            nombreArchivo,
            mimeType,
            contenido.length,
            "Acta de rúbrica firmada por el Directorio",
            identificadorIngreso,
            contenido
        );
        RadicacionDocumento guardada = repositorioRadicacionDocumento.save(acta);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(
            radicacion, null, radicacion.getEstado(),
            "Acta de rúbrica cargada: " + nombreArchivo,
            identificadorIngreso
        ));
        servicioAuditLog.registrarEvento(
            identificadorIngreso, "CARGA_ACTA_RUBRICA", "RadicacionDocumento",
            radicacion.getNumeroRadicado(),
            null,
            nombreArchivo,
            UtilRed.obtenerIpActual()
        );
        return guardada;
    }

    @Override
    public RadicacionDocumento obtenerActaRubrica(String identificadorIngreso, Long radicacionId) {
        obtenerPorId(identificadorIngreso, radicacionId);
        return repositorioRadicacionDocumento
            .findTopByRadicacionIdAndTipoDocumentoOrderByFechaSubidaDesc(radicacionId, TipoDocumentoRadicacion.ACTA_RUBRICA)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acta de rúbrica no encontrada para este expediente"));
    }

    private void crearProyectoDesdeRadicacion(RadicacionSolicitud radicacion, Usuario responsable) {
        String nombre = (radicacion.getProyectoProductivo() != null && !radicacion.getProyectoProductivo().isBlank())
            ? radicacion.getProyectoProductivo()
            : "Proyecto - " + radicacion.getEmpresa().getNombre();
        ProyectoProductivo proyecto = ProyectoProductivo.crear(
            nombre,
            radicacion.getDescripcion(),
            null,
            radicacion.getFechaPlazo(),
            responsable
        );
        proyecto.iniciarProyecto(radicacion);
        repositorioProyecto.save(proyecto);
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

        if (!verificarMagicBytes(extension, contenido)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El contenido del archivo no corresponde al formato declarado (" + extension + ")");
        }
    }

    private boolean verificarMagicBytes(String extension, byte[] contenido) {
        byte[][] firmas = MAGIC_BYTES.get(extension);
        if (firmas == null) return false;
        for (byte[] firma : firmas) {
            if (contenido.length < firma.length) continue;
            boolean coincide = true;
            for (int i = 0; i < firma.length; i++) {
                if (contenido[i] != firma[i]) {
                    coincide = false;
                    break;
                }
            }
            if (coincide) return true;
        }
        return false;
    }
}
