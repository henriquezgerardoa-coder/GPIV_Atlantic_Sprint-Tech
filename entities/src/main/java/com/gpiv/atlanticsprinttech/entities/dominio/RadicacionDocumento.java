package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "radicacion_documentos")
public class RadicacionDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "radicacion_id", nullable = false)
    private RadicacionSolicitud radicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumentoRadicacion tipoDocumento;

    @Column(name = "nombre_archivo", nullable = false, length = 200)
    private String nombreArchivo;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    @Column(name = "subido_por", nullable = false, length = 80)
    private String subidoPor;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "contenido", nullable = false)
    private byte[] contenido;

    protected RadicacionDocumento() {
    }

    private RadicacionDocumento(
        RadicacionSolicitud radicacion,
        TipoDocumentoRadicacion tipoDocumento,
        String nombreArchivo,
        String mimeType,
        long tamanoBytes,
        String descripcion,
        String subidoPor,
        byte[] contenido
    ) {
        this.radicacion = radicacion;
        this.tipoDocumento = tipoDocumento;
        this.nombreArchivo = nombreArchivo;
        this.mimeType = mimeType;
        this.tamanoBytes = tamanoBytes;
        this.descripcion = descripcion;
        this.subidoPor = subidoPor;
        this.fechaSubida = LocalDateTime.now();
        this.contenido = contenido;
    }

    public static RadicacionDocumento crear(
        RadicacionSolicitud radicacion,
        TipoDocumentoRadicacion tipoDocumento,
        String nombreArchivo,
        String mimeType,
        long tamanoBytes,
        String descripcion,
        String subidoPor,
        byte[] contenido
    ) {
        return new RadicacionDocumento(radicacion, tipoDocumento, nombreArchivo, mimeType, tamanoBytes, descripcion, subidoPor, contenido);
    }

    public Long getId() {
        return id;
    }

    public RadicacionSolicitud getRadicacion() {
        return radicacion;
    }

    public TipoDocumentoRadicacion getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getSubidoPor() {
        return subidoPor;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public byte[] getContenido() {
        return contenido;
    }
}
