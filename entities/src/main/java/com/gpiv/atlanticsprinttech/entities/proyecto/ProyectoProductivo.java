package com.gpiv.atlanticsprinttech.entities.proyecto;

import com.gpiv.atlanticsprinttech.entities.empresa.Empresa;
import com.gpiv.atlanticsprinttech.entities.lote.Lote;
import com.gpiv.atlanticsprinttech.entities.radicacion.EstadoRadicacion;
import com.gpiv.atlanticsprinttech.entities.radicacion.RadicacionSolicitud;
import com.gpiv.atlanticsprinttech.entities.shared.BusinessException;
import com.gpiv.atlanticsprinttech.entities.usuario.Usuario;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proyectos_productivos")
public class ProyectoProductivo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EstadoProyecto estado;

	@Column(nullable = false, length = 2000)
	private String descripcion;

	@Column(name = "fecha_inicio_real")
	private LocalDate fechaInicioReal;

	@Column(name = "fecha_estimada_fin", nullable = false)
	private LocalDate fechaEstimadaFin;

	@Column(name = "monto_inversion", nullable = false, precision = 15, scale = 2)
	private BigDecimal montoInversion;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lote_id", nullable = false)
	private Lote lote;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "responsable_seguimiento_id", nullable = false)
	private Usuario responsableSeguimiento;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "radicacion_origen_id")
	private RadicacionSolicitud radicacionOrigen;

	@OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<HitoObra> hitos = new ArrayList<>();

	@OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<DocumentoPDF> documentos = new ArrayList<>();

	@SuppressWarnings("unused")
	protected ProyectoProductivo() {
	}

	private ProyectoProductivo(
		String nombre,
		String descripcion,
		LocalDate fechaEstimadaFin,
		BigDecimal montoInversion,
		Empresa empresa,
		Lote lote,
		Usuario responsableSeguimiento
	) {
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.fechaEstimadaFin = fechaEstimadaFin;
		this.montoInversion = montoInversion;
		this.empresa = empresa;
		this.lote = lote;
		this.responsableSeguimiento = responsableSeguimiento;
		this.estado = EstadoProyecto.INICIADO;
		this.fechaCreacion = LocalDateTime.now();
	}

	public static ProyectoProductivo crear(
		String nombre,
		String descripcion,
		LocalDate fechaEstimadaFin,
		BigDecimal montoInversion,
		Empresa empresa,
		Lote lote,
		Usuario responsableSeguimiento
	) {
		return new ProyectoProductivo(nombre, descripcion, fechaEstimadaFin, montoInversion, empresa, lote, responsableSeguimiento);
	}

	public void iniciarProyecto(RadicacionSolicitud expediente) throws BusinessException {
		if (!EstadoRadicacion.RADICADA.equals(expediente.getEstado())) {
			throw new BusinessException("Solo se puede iniciar un proyecto desde una radicacion en estado RADICADA.");
		}
		if (this.estado != EstadoProyecto.INICIADO) {
			throw new BusinessException("El proyecto ya fue iniciado.");
		}
		this.radicacionOrigen = expediente;
		this.fechaInicioReal = LocalDate.now();
		this.estado = EstadoProyecto.EN_EJECUCION;
	}

	public void actualizarEstado(EstadoProyecto nuevoEstado) {
		if (this.estado.esFinal()) {
			throw new IllegalStateException("No se puede cambiar el estado de un proyecto finalizado.");
		}
		if (!this.estado.puedeTransicionarA(nuevoEstado)) {
			throw new IllegalArgumentException(
				"Transicion invalida: No se puede pasar de " + this.estado + " a " + nuevoEstado
			);
		}
		this.estado = nuevoEstado;
	}

	public double calcularAvanceFisico() {
		if (hitos.isEmpty()) return 0.0;
		long cumplidos = hitos.stream().filter(HitoObra::isCumplido).count();
		return (double) cumplidos / hitos.size() * 100.0;
	}

	public void registrarDocumentacionTecnica(DocumentoPDF doc) throws BusinessException {
		doc.validarExtension();
		doc.validarTamano();
		this.documentos.add(doc);
	}

	public boolean validarVencimientoPlazo() {
		return fechaEstimadaFin != null && LocalDate.now().isAfter(fechaEstimadaFin);
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public EstadoProyecto getEstado() {
		return estado;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public LocalDate getFechaInicioReal() {
		return fechaInicioReal;
	}

	public LocalDate getFechaEstimadaFin() {
		return fechaEstimadaFin;
	}

	public BigDecimal getMontoInversion() {
		return montoInversion;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public Lote getLote() {
		return lote;
	}

	public Usuario getResponsableSeguimiento() {
		return responsableSeguimiento;
	}

	public RadicacionSolicitud getRadicacionOrigen() {
		return radicacionOrigen;
	}

	public List<HitoObra> getHitos() {
		return hitos;
	}

	public List<DocumentoPDF> getDocumentos() {
		return documentos;
	}
}