package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_pdf")
public class DocumentoPDF {

    private static final long MAX_TAMANO_BYTES = 10L * 1024 * 1024; // 10 MB

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private ProyectoProductivo proyecto;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    protected DocumentoPDF() {
    }

    private DocumentoPDF(ProyectoProductivo proyecto, String nombreArchivo, Long tamanoBytes) {
        this.proyecto = proyecto;
        this.nombreArchivo = nombreArchivo;
        this.tamanoBytes = tamanoBytes;
        this.fechaCarga = LocalDateTime.now();
    }

    public static DocumentoPDF crear(ProyectoProductivo proyecto, String nombreArchivo, Long tamanoBytes) {
        DocumentoPDF doc = new DocumentoPDF(proyecto, nombreArchivo, tamanoBytes);
        doc.validarExtension();
        doc.validarTamano();
        return doc;
    }

    public Long getId() {
        return id;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public void validarTamano() {
        if (tamanoBytes != null && tamanoBytes > MAX_TAMANO_BYTES) {
            throw new IllegalStateException(
                "El documento supera el tamaño máximo permitido de 10 MB"
            );
        }
    }

    public void validarExtension() {
        if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".pdf")) {
            throw new IllegalStateException("El archivo debe tener extensión .pdf");
        }
    }
}
