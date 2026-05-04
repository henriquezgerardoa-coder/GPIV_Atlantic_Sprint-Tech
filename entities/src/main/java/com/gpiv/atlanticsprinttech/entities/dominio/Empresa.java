package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.*;

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

	@Column(length = 50)
	private String telefono;

	@Column(name = "cantidad_empleados")
	private int cantidadEmpleados;

	@Column(name = "esta_activa", nullable = false)
	private boolean estaActiva;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "rubro_id")
	private Rubro rubro;

	protected Empresa() {
	}

	private Empresa(String nombreEmpresa, String cuit, String correoElectronico) {
		this.nombre = nombreEmpresa;
		this.cuit = cuit;
		this.correoElectronico = correoElectronico;
	}
	private Empresa(String nombreEmpresa, String cuit, String correoElectronico, String telefono, Rubro rubro) {
		this.nombre = nombreEmpresa;
		this.cuit = cuit;
		this.correoElectronico = correoElectronico;
		this.telefono = telefono;
		this.rubro = rubro;
		this.cantidadEmpleados = 0;
		this.estaActiva = true;
	}

	public static Empresa crear(String nombre, String cuit, String correoElectronico) {
		return new Empresa(nombre, cuit, correoElectronico);
	}
	public static Empresa crear(String nombre, String cuit, String correoElectronico, String telefono, Rubro rubro) {
		return new Empresa(nombre, cuit, correoElectronico, telefono, rubro);
	}

	// Getters (ver si se pueden revocar. Refactorizar)
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
	public Rubro getRubro() {
		return rubro;
	}
	public int getCantidadEmpleados() {
		return  cantidadEmpleados;
	}

	//netodos

	//R09(Autogestion sin CUIT/Nombre)
	public void actualizarDatosContacto(String nuevoCorreoElectronico, String nuevoTelefono) {
		if(nuevoCorreoElectronico != null && !nuevoCorreoElectronico.isBlank()) {
			this.correoElectronico = nuevoCorreoElectronico;
		}
		if(nuevoTelefono != null && !nuevoTelefono.isBlank()) {
			this.telefono = nuevoTelefono;
		}
	}

	public void solicitarCambioRubro(Rubro nuevoRubro, String justificacion) {
		if (this.rubro.equals(nuevoRubro)) {
			throw new IllegalArgumentException("El nuevo rubro no puede ser igual al actual.");
		}
		//TODO: Clase externa see encarga de la solicitud del cambio de rubro
		//SolicitudRadicacion.crearSolicitudCambio(this, nuevoRubro, justificacion);
	}



}