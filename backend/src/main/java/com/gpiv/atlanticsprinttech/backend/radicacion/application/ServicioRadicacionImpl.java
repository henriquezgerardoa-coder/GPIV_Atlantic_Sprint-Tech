package com.gpiv.atlanticsprinttech.backend.radicacion.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpiv.atlanticsprinttech.backend.empresa.persistence.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.lote.persistence.RepositorioLote;
import com.gpiv.atlanticsprinttech.backend.proyecto.persistence.RepositorioProyecto;
import com.gpiv.atlanticsprinttech.backend.radicacion.persistence.RepositorioRadicacionDocumento;
import com.gpiv.atlanticsprinttech.backend.radicacion.persistence.RepositorioRadicacionHistorial;
import com.gpiv.atlanticsprinttech.backend.correo.application.ServicioCorreo;
import com.gpiv.atlanticsprinttech.backend.radicacion.persistence.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.backend.usuario.persistence.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.entities.compartido.ExcepcionNegocio;
import com.gpiv.atlanticsprinttech.entities.lote.EstadoAsignacionLote;
import com.gpiv.atlanticsprinttech.entities.lote.Lote;
import com.gpiv.atlanticsprinttech.commons.radicacion.dto.SolicitudRelevamientoPedidoLotes;
import com.gpiv.atlanticsprinttech.entities.empresa.Empresa;
import com.gpiv.atlanticsprinttech.entities.radicacion.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionDocumento;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionHistorial;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.radicacion.TipoDocumentoRadicacion;
import com.gpiv.atlanticsprinttech.entities.usuario.Usuario;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.gpiv.atlanticsprinttech.entities.proyecto.ProyectoProductivo;
import com.gpiv.atlanticsprinttech.entities.proyecto.EstadoProyecto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    private final RepositorioRadicacionSolicitud repositorioRadicacionSolicitud;
    private final RepositorioRadicacionHistorial repositorioRadicacionHistorial;
    private final RepositorioRadicacionDocumento repositorioRadicacionDocumento;
    private final RepositorioEmpresa repositorioEmpresa;
    private final RepositorioLote repositorioLote;
    private final ServicioContextoUsuario servicioContextoUsuario;
    private final ObjectMapper objectMapper;
    private final ServicioCorreo servicioCorreo;
    private final RepositorioProyecto repositorioProyecto;
    private final RepositorioUsuario repositorioUsuario;

    public ServicioRadicacionImpl(
        RepositorioRadicacionSolicitud repositorioRadicacionSolicitud,
        RepositorioRadicacionHistorial repositorioRadicacionHistorial,
        RepositorioRadicacionDocumento repositorioRadicacionDocumento,
        RepositorioEmpresa repositorioEmpresa,
        RepositorioLote repositorioLote,
        ServicioContextoUsuario servicioContextoUsuario,
        ObjectMapper objectMapper,
        ServicioCorreo servicioCorreo,
        RepositorioProyecto repositorioProyecto,
        RepositorioUsuario repositorioUsuario
    ) {
        this.repositorioRadicacionSolicitud = repositorioRadicacionSolicitud;
        this.repositorioRadicacionHistorial = repositorioRadicacionHistorial;
        this.repositorioRadicacionDocumento = repositorioRadicacionDocumento;
        this.repositorioEmpresa = repositorioEmpresa;
        this.repositorioLote = repositorioLote;
        this.servicioContextoUsuario = servicioContextoUsuario;
        this.objectMapper = objectMapper;
        this.servicioCorreo = servicioCorreo;
        this.repositorioProyecto = repositorioProyecto;
        this.repositorioUsuario = repositorioUsuario;
    }

    @Override
    public Page<RadicacionSolicitud> listar(String identificadorIngreso, EstadoRadicacion estado, LocalDate desde, LocalDate hasta, Pageable pageable) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        Long empresaId = null;
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            if (usuario.getEmpresaId() == null) {
                return Page.empty(pageable);
            }
            empresaId = usuario.getEmpresaId();
        }
        return repositorioRadicacionSolicitud.buscarFiltrado(empresaId, estado, desde, hasta, pageable);
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
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(guardada, null, guardada.getEstado(), "Solicitud creada", identificadorIngreso));
        String correoDestino = (relevamientoPedidoLotes != null && relevamientoPedidoLotes.correoElectronico() != null)
            ? relevamientoPedidoLotes.correoElectronico()
            : empresa.getCorreoElectronico();
        if (correoDestino != null && !correoDestino.isBlank()) {
            servicioCorreo.enviarConfirmacion(correoDestino, guardada.getNumeroRadicado());
        }
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
        if (!validarDigitoVerificadorCuit(cuitDigitos)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El CUIT no es valido (digito verificador incorrecto)");
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
    public RadicacionSolicitud cambiarEstado(String identificadorIngreso, Long id, EstadoRadicacion nuevoEstado, String comentario) {
        Usuario usuarioActual = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (nuevoEstado == EstadoRadicacion.RECHAZADA && (comentario == null || comentario.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo de rechazo es obligatorio");
        }
        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, id);
        EstadoRadicacion estadoAnterior = radicacion.getEstado();
        try {
            radicacion.cambiarEstado(nuevoEstado);
            if (nuevoEstado == EstadoRadicacion.APROBADA) {

                ProyectoProductivo proyecto = new ProyectoProductivo();
                proyecto.setNombre("PROY-" + radicacion.getNumeroRadicado());
                proyecto.setDescripcion(radicacion.getDescripcion());

                if (radicacion.getRentabilidadEstimada() != null) {
                    proyecto.setMontoInversion(BigDecimal.valueOf(radicacion.getRentabilidadEstimada()));
                } else {
                    proyecto.setMontoInversion(BigDecimal.ZERO);
                }

                proyecto.setEstado(EstadoProyecto.INICIADO);
                proyecto.setFechaCreacion(LocalDateTime.now());
                proyecto.setEmpresa(radicacion.getEmpresa());
                proyecto.setRadicacionOrigen(radicacion);

                Usuario admin = repositorioUsuario.findByNombreUsuario(identificadorIngreso)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Usuario no encontrado"));
                proyecto.setResponsableSeguimiento(admin);

                repositorioProyecto.save(proyecto);
            }
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        RadicacionHistorial historial = RadicacionHistorial.crear(
                radicacion,
                estadoAnterior,
                nuevoEstado,
                comentario,
                usuarioActual.getNombreUsuario()
        );
        repositorioRadicacionHistorial.save(historial);
        return repositorioRadicacionSolicitud.save(radicacion);
    }

    @Override
    public void registrarObservacion(String identificadorIngreso, Long id, String comentario) {
        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, id);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(radicacion, null, radicacion.getEstado(), comentario, identificadorIngreso));
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
        return guardado;
    }

    @Override
    public List<RadicacionDocumento> listarDocumentos(String identificadorIngreso, Long id) {
        obtenerPorId(identificadorIngreso, id);
        return repositorioRadicacionDocumento.findByRadicacionIdOrderByFechaSubidaDesc(id);
    }

    @Override
    public List<RadicacionHistorial> listarHistorial(String identificadorIngreso, Long id) {
        obtenerPorId(identificadorIngreso, id);
        return repositorioRadicacionHistorial.findByRadicacionIdOrderByFechaEventoDesc(id);
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

        // Escaneo basico: bloquea payloads con patrones de script embebido.
        String muestra = new String(contenido, 0, Math.min(contenido.length, 2048));
        String muestraNormalizada = muestra.toLowerCase(Locale.ROOT);
        if (muestraNormalizada.contains("<script") || muestraNormalizada.contains("javascript:")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo rechazado por validacion de seguridad");
        }
    }

    @Override
    public void asignarLote(String identificadorIngreso, Long radicacionId, Long loteId) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (!usuario.esAdministrador()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el administrador puede asignar lotes a radicaciones");
        }
        RadicacionSolicitud radicacion = repositorioRadicacionSolicitud.findByIdConEmpresa(radicacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Radicacion no encontrada"));

        Lote lote = repositorioLote.findByIdConEmpresa(loteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));

        try {
            lote.preAdjudicarALaSolicitud(radicacion.getNumeroRadicado());
        } catch (ExcepcionNegocio ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        repositorioLote.save(lote);
        repositorioRadicacionHistorial.save(
            RadicacionHistorial.crear(radicacion, null, radicacion.getEstado(),
                "Lote " + lote.getCodigo() + " reservado para esta solicitud", identificadorIngreso)
        );
    }

    @Override
    public Optional<Lote> obtenerLoteAsignado(String identificadorIngreso, Long radicacionId) {
        RadicacionSolicitud radicacion = repositorioRadicacionSolicitud.findByIdConEmpresa(radicacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Radicacion no encontrada"));
        return repositorioLote.findByNumeroExpedienteReferenciaAndEstadoAsignacion(
            radicacion.getNumeroRadicado(), EstadoAsignacionLote.PREADJUDICADO_A_SOLICITUD
        );
    }

    private boolean validarDigitoVerificadorCuit(String cuit11Digitos) {
        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(cuit11Digitos.charAt(i)) * pesos[i];
        }
        int resto = suma % 11;
        if (resto == 1) {
            return false;
        }
        int digitoEsperado = (resto == 0) ? 0 : (11 - resto);
        return Character.getNumericValue(cuit11Digitos.charAt(10)) == digitoEsperado;
    }
}
