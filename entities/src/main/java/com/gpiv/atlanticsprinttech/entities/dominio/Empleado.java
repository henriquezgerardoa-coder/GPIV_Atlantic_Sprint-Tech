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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "empleados", uniqueConstraints = {
	@UniqueConstraint(name = "uk_empleado_empresa_cuit", columnNames = {"empresa_id", "cuit"})
})
public class Empleado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String cuit;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(name = "fecha_registro", nullable = false)
	private LocalDateTime fechaRegistro;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	protected Empleado() {
	}

	private Empleado(String cuit, String nombre, Empresa empresa) {
		this.cuit = cuit;
		this.nombre = nombre;
		this.empresa = empresa;
		this.fechaRegistro = LocalDateTime.now();
	}

	public static Empleado crear(String cuit, String nombre, Empresa empresa) {
		return new Empleado(cuit, nombre, empresa);
	}

	public Long getId() {
		return id;
	}

	public String getCuit() {
		return cuit;
	}

	public String getNombre() {
		return nombre;
	}

	public LocalDateTime getFechaRegistro() {
		return fechaRegistro;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public Long getEmpresaId() {
		return empresa == null ? null : empresa.getId();
	}

	public void actualizarDatos(String cuit, String nombre) {
		this.cuit = cuit;
		this.nombre = nombre;
	}
}

