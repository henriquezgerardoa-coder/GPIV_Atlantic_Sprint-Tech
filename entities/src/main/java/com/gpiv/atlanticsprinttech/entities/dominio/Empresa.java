package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "empresas", uniqueConstraints = {
	@UniqueConstraint(name = "uk_empresa_cuit", columnNames = "cuit")
})
public class Empresa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false, length = 120)
	private String nombre;
	@Column(nullable = false, length = 20, unique = true)
	private String cuit;
	@Column(name = "correo_electronico", nullable = false, length = 120)
	private String correoElectronico;
	protected Empresa() {
	}
	private Empresa(String nombre, String cuit, String correoElectronico) {
		this.nombre = nombre;
		this.cuit = cuit;
		this.correoElectronico = correoElectronico;
	}
	public static Empresa crear(String nombre, String cuit, String correoElectronico) {
		return new Empresa(nombre, cuit, correoElectronico);
	}
	public Long getId() {
		return id;
	}
	public String getNombre() {
		return nombre;
	}
	public String getCuit() {
		return cuit;
	}
	public String getCorreoElectronico() {
		return correoElectronico;
	}
	public void actualizarDatos(String nombre, String cuit, String correoElectronico) {
		this.nombre = nombre;
		this.cuit = cuit;
		this.correoElectronico = correoElectronico;
	}
}