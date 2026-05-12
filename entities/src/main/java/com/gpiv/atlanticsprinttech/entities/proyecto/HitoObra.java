package com.gpiv.atlanticsprinttech.entities.proyecto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "hitos_obra")
public class HitoObra {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 500)
	private String descripcion;

	@Column(name = "fecha_vencimiento_real", nullable = false)
	private LocalDate fechaVencimientoReal;

	@Column(nullable = false)
	private boolean cumplido;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "proyecto_id", nullable = false)
	private ProyectoProductivo proyecto;

	@SuppressWarnings("unused")
	protected HitoObra() {
	}

	private HitoObra(String descripcion, LocalDate fechaVencimientoReal, ProyectoProductivo proyecto) {
		this.descripcion = descripcion;
		this.fechaVencimientoReal = fechaVencimientoReal;
		this.proyecto = proyecto;
		this.cumplido = false;
	}

	public static HitoObra crear(String descripcion, LocalDate fechaVencimientoReal, ProyectoProductivo proyecto) {
		return new HitoObra(descripcion, fechaVencimientoReal, proyecto);
	}

	public boolean estaVencido() {
		return !cumplido && LocalDate.now().isAfter(fechaVencimientoReal);
	}

	public void marcaComoCumplido() {
		this.cumplido = true;
	}

	public Long getId() {
		return id;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public LocalDate getFechaVencimientoReal() {
		return fechaVencimientoReal;
	}

	public boolean estaCumplido() {
		return cumplido;
	}

	public ProyectoProductivo getProyecto() {
		return proyecto;
	}
}