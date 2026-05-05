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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "radicaciones", uniqueConstraints = {
	@UniqueConstraint(name = "uk_radicacion_numero", columnNames = "numero_radicado")
})
public class RadicacionSolicitud {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "numero_radicado", nullable = false, length = 40, unique = true)
	private String numeroRadicado;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column(name = "tipo_solicitud", nullable = false, length = 80)
	private String tipoSolicitud;

	@Column(name = "descripcion", nullable = false, length = 1000)
	private String descripcion;

	@Column(name = "uso_estimativo", length = 120)
	private String usoEstimativo;

	@Column(name = "relevamiento_pedido_lotes", length = 12000)
	private String relevamientoPedidoLotesJson;

	@Column(name = "fecha_radicacion", nullable = false)
	private LocalDate fechaRadicacion;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 40)
	private EstadoRadicacion estado;

	@Column(name = "fecha_ultima_actualizacion", nullable = false)
	private LocalDateTime fechaUltimaActualizacion;

	protected RadicacionSolicitud() {
	}

	private RadicacionSolicitud(
		String numeroRadicado,
		Empresa empresa,
		String tipoSolicitud,
		String descripcion,
		String usoEstimativo,
		String relevamientoPedidoLotesJson
	) {
		this.numeroRadicado = numeroRadicado;
		this.empresa = empresa;
		this.tipoSolicitud = tipoSolicitud;
		this.descripcion = descripcion;
		this.usoEstimativo = usoEstimativo;
		this.relevamientoPedidoLotesJson = relevamientoPedidoLotesJson;
		this.fechaRadicacion = LocalDate.now();
		this.estado = EstadoRadicacion.PENDIENTE;
	}

	public static RadicacionSolicitud crear(
		String numeroRadicado,
		Empresa empresa,
		String tipoSolicitud,
		String descripcion,
		String usoEstimativo,
		String relevamientoPedidoLotesJson
	) {
		return new RadicacionSolicitud(numeroRadicado, empresa, tipoSolicitud, descripcion, usoEstimativo, relevamientoPedidoLotesJson);
	}

	@PrePersist
	@PreUpdate
	void actualizarMarcaTiempo() {
		this.fechaUltimaActualizacion = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getNumeroRadicado() {
		return numeroRadicado;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public String getTipoSolicitud() {
		return tipoSolicitud;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public String getUsoEstimativo() {
		return usoEstimativo;
	}

	public String getRelevamientoPedidoLotesJson() {
		return relevamientoPedidoLotesJson;
	}

	public LocalDate getFechaRadicacion() {
		return fechaRadicacion;
	}

	public EstadoRadicacion getEstado() {
		return estado;
	}

	public LocalDateTime getFechaUltimaActualizacion() {
		return fechaUltimaActualizacion;
	}

	public void cambiarEstado(EstadoRadicacion estado) {
		this.estado = estado;
	}
}

