package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionDocumento;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionHistorial;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioRadicacionSolicitud;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioRadicacion;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionDocumento;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionHistorial;
import com.gpiv.atlanticsprinttech.entities.dominio.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.dominio.TipoDocumentoRadicacion;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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

    private final RepositorioRadicacionSolicitud repositorioRadicacionSolicitud;
    private final RepositorioRadicacionHistorial repositorioRadicacionHistorial;
    private final RepositorioRadicacionDocumento repositorioRadicacionDocumento;
    private final RepositorioEmpresa repositorioEmpresa;
    private final ServicioContextoUsuario servicioContextoUsuario;

    public ServicioRadicacionImpl(
        RepositorioRadicacionSolicitud repositorioRadicacionSolicitud,
        RepositorioRadicacionHistorial repositorioRadicacionHistorial,
        RepositorioRadicacionDocumento repositorioRadicacionDocumento,
        RepositorioEmpresa repositorioEmpresa,
        ServicioContextoUsuario servicioContextoUsuario
    ) {
        this.repositorioRadicacionSolicitud = repositorioRadicacionSolicitud;
        this.repositorioRadicacionHistorial = repositorioRadicacionHistorial;
        this.repositorioRadicacionDocumento = repositorioRadicacionDocumento;
        this.repositorioEmpresa = repositorioEmpresa;
        this.servicioContextoUsuario = servicioContextoUsuario;
    }

    @Override
    public List<RadicacionSolicitud> listar(String identificadorIngreso, EstadoRadicacion estado, LocalDate desde, LocalDate hasta) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        Long empresaId = servicioContextoUsuario.esRolEmpresa(usuario)
            ? servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario)
            : null;
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
    public RadicacionSolicitud crear(String identificadorIngreso, String tipoSolicitud, String descripcion) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        Long empresaId = servicioContextoUsuario.obtenerEmpresaIdRequerido(usuario);
        Empresa empresa = repositorioEmpresa.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La empresa asociada no existe"));

        String numero = generarNumeroRadicado();
        RadicacionSolicitud nueva = RadicacionSolicitud.crear(numero, empresa, tipoSolicitud.trim(), descripcion.trim());
        RadicacionSolicitud guardada = repositorioRadicacionSolicitud.save(nueva);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(guardada, guardada.getEstado(), "Solicitud creada", identificadorIngreso));
        return guardada;
    }

    @Override
    public RadicacionSolicitud cambiarEstado(String identificadorIngreso, Long id, EstadoRadicacion estado, String comentario) {
        Usuario usuario = servicioContextoUsuario.obtenerUsuarioPorIngreso(identificadorIngreso);
        if (servicioContextoUsuario.esRolEmpresa(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El rol EMPRESA no puede cambiar el estado");
        }

        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, id);
        radicacion.cambiarEstado(estado);
        RadicacionSolicitud actualizada = repositorioRadicacionSolicitud.save(radicacion);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(actualizada, estado, comentario, identificadorIngreso));
        return actualizada;
    }

    @Override
    public void registrarObservacion(String identificadorIngreso, Long id, String comentario) {
        RadicacionSolicitud radicacion = obtenerPorId(identificadorIngreso, id);
        repositorioRadicacionHistorial.save(RadicacionHistorial.crear(radicacion, radicacion.getEstado(), comentario, identificadorIngreso));
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
}
