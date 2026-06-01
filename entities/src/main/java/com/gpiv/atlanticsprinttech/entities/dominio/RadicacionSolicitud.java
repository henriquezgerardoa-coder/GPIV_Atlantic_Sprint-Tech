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

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "lote_id", nullable = true)
	private Lote lote;

	@Column(name = "tipo_solicitud", nullable = false, length = 80)
	private String tipoSolicitud;

	@Column(name = "descripcion", nullable = false, length = 1000)
	private String descripcion;

	@Column(name = "uso_estimativo", length = 120)
	private String usoEstimativo;

	@Column(name = "relevamiento_pedido_lotes", length = 12000)
	private String relevamientoPedidoLotesJson;

	@Column(name = "proyecto_productivo", length = 120)
	private String proyectoProductivo;

	@Column(name = "rentabilidad_estimada")
	private Double rentabilidadEstimada;

	@Column(name = "empleados_previstos")
	private Integer empleadosPrevistos;

	@Column(name = "plan_ambiental", length = 500)
	private String planAmbiental;

	@Column(name = "tiempo_estimado_obra_meses")
	private Integer tiempoEstimadoObraMeses;

	@Column(name = "fecha_plazo")
	private LocalDate fechaPlazo;

	@Column(name = "fecha_aprobacion")
	private LocalDate fechaAprobacion;

	@Column(name = "fecha_radicacion", nullable = false)
	private LocalDate fechaRadicacion;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 40)
	private EstadoRadicacion estado;

	@Column(name = "numero_resolucion", length = 50)
	private String numeroResolucion;

	@Column(name = "resuelto_por", length = 80)
	private String resueltoPor;

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

	public String getProyectoProductivo() {
		return proyectoProductivo;
	}

	public Double getRentabilidadEstimada() {
		return rentabilidadEstimada;
	}

	public Integer getEmpleadosPrevistos() {
		return empleadosPrevistos;
	}

	public String getPlanAmbiental() {
		return planAmbiental;
	}

	public Integer getTiempoEstimadoObraMeses() {
		return tiempoEstimadoObraMeses;
	}

	public LocalDate getFechaPlazo() {
		return fechaPlazo;
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

	// Retorna el estado previo para auditoría e historial.
	public EstadoRadicacion aplicarTransicion(
		EstadoRadicacion nuevoEstado,
		String comentario,
		LocalDate nuevaFechaPlazo,
		Integer nuevoTiempoObraMeses,
		LocalDate fechaAprobacionExplicita,
		String numeroResolucion,
		String responsable
	) {
		EstadoRadicacion anterior = this.estado;
		if (nuevoEstado == EstadoRadicacion.APROBADA && fechaAprobacionExplicita != null) {
			this.fechaAprobacion = fechaAprobacionExplicita;
		}
		validarYCambiarEstado(nuevoEstado, comentario, nuevaFechaPlazo);
		if (nuevoTiempoObraMeses != null || nuevaFechaPlazo != null) {
			establecerDatosPlazo(nuevoTiempoObraMeses, nuevaFechaPlazo);
		}
		if (nuevoEstado.requiereRegistrarResolucion()) {
			establecerResolucion(numeroResolucion, responsable);
		}
		sincronizarLote(this.lote);
		return anterior;
	}

	public void validarYCambiarEstado(EstadoRadicacion nuevo, String comentario, LocalDate fechaPlazo) {
		if (this.estado == nuevo) {
			throw new IllegalStateException("La radicacion ya se encuentra en ese estado");
		}
		if (!this.estado.puedeTransicionarA(nuevo)) {
			throw new IllegalStateException("Transicion de estado no permitida");
		}
		if (nuevo.requiereComentario() && (comentario == null || comentario.isBlank())) {
			throw new IllegalArgumentException("Debe indicar un comentario para el estado seleccionado");
		}
		if (nuevo == EstadoRadicacion.APROBADA && fechaPlazo == null) {
			throw new IllegalArgumentException("Debe indicar la fecha de plazo al aprobar una solicitud");
		}
		if (nuevo == EstadoRadicacion.RADICADA && this.fechaPlazo != null
				&& LocalDate.now().isAfter(this.fechaPlazo)) {
			throw new IllegalStateException(
				"No se puede radicar: el plazo de radicacion ha vencido (plazo: " + this.fechaPlazo + ")"
			);
		}
		cambiarEstado(nuevo);
	}

	public void validarPermiteAsignarLote() {
		if (!this.estado.permiteAsignarLote()) {
			throw new IllegalStateException("No se puede asignar un lote a una solicitud rechazada o cancelada");
		}
	}

	public void validarPermiteActaRubrica() {
		if (!this.estado.permiteActaRubrica()) {
			throw new IllegalStateException("El Acta de Rubrica solo puede cargarse cuando el expediente esta Aprobado o Radicado");
		}
	}

	public void sincronizarLote(Lote lote) {
		if (lote == null) return;
		if (estado == EstadoRadicacion.APROBADA || estado == EstadoRadicacion.RADICADA) {
			if (lote.getEtapa().puedeTransicionarA(EtapaCicloLote.ADJUDICADO_PRECARIO)) {
				lote.transicionar(EtapaCicloLote.ADJUDICADO_PRECARIO);
			}
		} else if (estado == EstadoRadicacion.RECHAZADA || estado == EstadoRadicacion.CANCELADA
				|| estado == EstadoRadicacion.DESADJUDICACION) {
			if (lote.getEtapa().puedeTransicionarA(EtapaCicloLote.REVERTIDO)) {
				lote.transicionar(EtapaCicloLote.REVERTIDO);
			}
		}
	}

	private void cambiarEstado(EstadoRadicacion estado) {
		this.estado = estado;
		if (estado == EstadoRadicacion.APROBADA && this.fechaAprobacion == null) {
			this.fechaAprobacion = LocalDate.now();
		}
	}

	public LocalDate getFechaAprobacion() {
		return fechaAprobacion;
	}

	public String getNumeroResolucion() {
		return numeroResolucion;
	}

	public String getResueltoPor() {
		return resueltoPor;
	}

	public void establecerResolucion(String numeroResolucion, String resueltoPor) {
		if (numeroResolucion != null && !numeroResolucion.isBlank()) {
			this.numeroResolucion = numeroResolucion.trim();
		}
		this.resueltoPor = resueltoPor;
	}

	public Lote getLote() {
		return lote;
	}

	public Long obtenerIdLote() {
		return lote != null ? lote.getId() : null;
	}

	public String obtenerCodigoLote() {
		return lote != null ? lote.getCodigo() : null;
	}

	public void asignarLote(Lote lote) {
		this.lote = lote;
	}

	public void establecerDatosProyecto(
		String proyectoProductivo,
		Double rentabilidadEstimada,
		Integer empleadosPrevistos,
		String planAmbiental
	) {
		this.proyectoProductivo = proyectoProductivo;
		this.rentabilidadEstimada = rentabilidadEstimada;
		this.empleadosPrevistos = empleadosPrevistos;
		this.planAmbiental = planAmbiental;
	}

	public void establecerFechaAprobacion(LocalDate fecha) {
		this.fechaAprobacion = fecha;
	}

	public void establecerDatosPlazo(Integer tiempoEstimadoObraMeses, LocalDate fechaPlazo) {
		this.tiempoEstimadoObraMeses = tiempoEstimadoObraMeses;
		this.fechaPlazo = fechaPlazo;
	}

        public boolean esPlazoVencido() {
                return fechaPlazo != null
                        && fechaPlazo.isBefore(LocalDate.now())
                        && !estado.esFinal();
        }

        public String getFechaPlazoTexto() {
                return fechaPlazo != null ? fechaPlazo.toString() : null;
        }

        public String getFechaAprobacionTexto() {
                return fechaAprobacion != null ? fechaAprobacion.toString() : null;
        }

        public String getFechaRadicacionTexto() {
                return fechaRadicacion != null ? fechaRadicacion.toString() : null;
        }

        public String getFechaUltimaActualizacionTexto() {
                return fechaUltimaActualizacion != null ? fechaUltimaActualizacion.toString() : null;
        }

        public boolean validarCuit() {
                String cuit = empresa.getCuit();
                if (cuit == null || cuit.isBlank()) return false;
                return cuit.matches("\\d{2}-\\d{8}-\\d");
        }
}

