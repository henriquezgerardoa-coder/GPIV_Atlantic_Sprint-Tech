package com.gpiv.atlanticsprinttech.entities.proyecto;

import com.gpiv.atlanticsprinttech.entities.shared.BusinessException;
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

	private static final long MAX_TAMANO_BYTES = 20L * 1024 * 1024;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nombre_archivo", nullable = false, length = 200)
	private String nombreArchivo;

	@Column(name = "fecha_carga", nullable = false)
	private LocalDateTime fechaCarga;

	@Column(name = "tamano_bytes", nullable = false)
	private long tamanoBytes;

	@Column(name = "contenido", nullable = false)
	private byte[] contenido;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "proyecto_id", nullable = false)
	private ProyectoProductivo proyecto;

	@SuppressWarnings("unused")
	protected DocumentoPDF() {
	}

	private DocumentoPDF(String nombreArchivo, long tamanoBytes, byte[] contenido, ProyectoProductivo proyecto) {
		this.nombreArchivo = nombreArchivo;
		this.tamanoBytes = tamanoBytes;
		this.contenido = contenido;
		this.proyecto = proyecto;
		this.fechaCarga = LocalDateTime.now();
	}

	public static DocumentoPDF crear(
		String nombreArchivo,
		long tamanoBytes,
		byte[] contenido,
		ProyectoProductivo proyecto
	) throws BusinessException {
		DocumentoPDF doc = new DocumentoPDF(nombreArchivo, tamanoBytes, contenido, proyecto);
		doc.validarExtension();
		doc.validarTamano();
		return doc;
	}

	public void validarExtension() throws BusinessException {
		if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".pdf")) {
			throw new BusinessException("El documento debe tener extension .pdf: " + nombreArchivo);
		}
	}

	public void validarTamano() throws BusinessException {
		if (tamanoBytes <= 0 || tamanoBytes > MAX_TAMANO_BYTES) {
			throw new BusinessException("El tamano del documento excede el limite de 20 MB.");
		}
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

	public long getTamanoBytes() {
		return tamanoBytes;
	}

	public byte[] getContenido() {
		return contenido;
	}

	public ProyectoProductivo getProyecto() {
		return proyecto;
	}
}