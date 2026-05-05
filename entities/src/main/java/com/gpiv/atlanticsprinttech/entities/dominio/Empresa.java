package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresas", uniqueConstraints = {
		@UniqueConstraint(name = "uk_empresa_cuit", columnNames = "cuit"),
		@UniqueConstraint(name = "uk_empresa_nit", columnNames = "nit")
})
public class Empresa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(name = "razon_social", nullable = false, length = 160)
	private String razonSocial;

	@Column(name = "nit", nullable = false, length = 30, unique = true)
	private String nit;

	@Column(nullable = false, length = 20, unique = true)
	private String cuit;

	@Column(name = "direccion", nullable = false, length = 240)
	private String direccion;

	@Column(name = "actividad_economica", nullable = false, length = 180)
	private String actividadEconomica;

	@Column(name = "correo_electronico", nullable = false, length = 120)
	private String correoElectronico;

	@Column(name = "telefono", length = 40)
	private String telefono;

	// --- INTEGRACIÓN: Requerimiento de tu asignación ---
	@Column(name = "representante_legal", length = 120)
	private String representanteLegal;

	@Column(name = "fecha_registro", nullable = false)
	private LocalDateTime fechaRegistro;

	@Column(name = "status", nullable = false, length = 30)
	private String status;

	@Column(name = "cantidad_empleados")
	private Integer cantidadEmpleados;

	@Column(name = "vehiculos_asignados_json", length = 12000)
	private String vehiculosAsignadosJson;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "rubro_id")
	private Rubro rubro;

	protected Empresa() {
	}
	private Empresa(
			String nombre,
			String razonSocial,
			String nit,
			String cuit,
			String direccion,
			String actividadEconomica,
			String correoElectronico,
			String telefono,
			String representanteLegal, // Nuevo
			Integer cantidadEmpleados,
			String vehiculosAsignadosJson,
			Rubro rubro // Nuevo
	) {
		this.nombre = nombre;
		this.razonSocial = razonSocial;
		this.nit = nit;
		this.cuit = cuit;
		this.direccion = direccion;
		this.actividadEconomica = actividadEconomica;
		this.correoElectronico = correoElectronico;
		this.telefono = telefono;
		this.representanteLegal = representanteLegal;
		this.fechaRegistro = LocalDateTime.now();
		this.status = "ACTIVA";
		this.cantidadEmpleados = cantidadEmpleados;
		this.vehiculosAsignadosJson = vehiculosAsignadosJson;
		this.rubro = rubro;
	}

	public static Empresa crear(
			String nombre, String razonSocial, String nit, String cuit,
			String direccion, String actividadEconomica, String correoElectronico, String telefono
	) {
		return new Empresa(
				nombre, razonSocial, nit, cuit, direccion, actividadEconomica,
				correoElectronico, telefono, null, 0, null, null
		);
	}

	public static Empresa crear(String nombre, String cuit, String correoElectronico) {
		return new Empresa(nombre, nombre, cuit, cuit, "", "", correoElectronico, null, null, 0, null, null);
	}


	public static Empresa crearConRubro(
			String nombre, String razonSocial, String nit, String cuit,
			String direccion, String actividadEconomica, String correoElectronico,
			String telefono, String representanteLegal, Rubro rubro
	) {
		return new Empresa(
				nombre, razonSocial, nit, cuit, direccion, actividadEconomica,
				correoElectronico, telefono, representanteLegal, 0, null, rubro
		);
	}

	public Long getId() { return id; }
	public String getNombre() { return nombre; }
	public String getCuit() { return cuit; }
	public String getRazonSocial() { return razonSocial; }
	public String getNit() { return nit; }
	public String getDireccion() { return direccion; }
	public String getActividadEconomica() { return actividadEconomica; }
	public String getCorreoElectronico() { return correoElectronico; }
	public String getTelefono() { return telefono; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public String getStatus() { return status; }
	public Integer getCantidadEmpleados() { return cantidadEmpleados; }
	public String getVehiculosAsignadosJson() { return vehiculosAsignadosJson; }

	public String getRepresentanteLegal() { return representanteLegal; } // Nuevo
	public Rubro getRubro() { return rubro; } // Nuevo

	public void actualizarDatos(
			String nombre, String razonSocial, String nit, String cuit,
			String direccion, String actividadEconomica, String correoElectronico, String telefono
	) {
		this.nombre = nombre;
		this.razonSocial = razonSocial;
		this.nit = nit;
		this.cuit = cuit;
		this.direccion = direccion;
		this.actividadEconomica = actividadEconomica;
		this.correoElectronico = correoElectronico;
		this.telefono = telefono;
	}

	public void actualizarDatos(String nombre, String cuit, String correoElectronico) {
		actualizarDatos(nombre, nombre, cuit, cuit, this.direccion, this.actividadEconomica, correoElectronico, this.telefono);
	}

	public void actualizarServiciosPostRadicacion(Integer cantidadEmpleados, String vehiculosAsignadosJson) {
		this.cantidadEmpleados = cantidadEmpleados;
		this.vehiculosAsignadosJson = vehiculosAsignadosJson;
	}
	// Requerimiento R09: La empresa solo puede autogestionar su contacto y representante
	public void actualizarDatosContacto(String nuevoCorreoElectronico, String nuevoTelefono, String nuevoRepresentante) {
		if (nuevoCorreoElectronico != null && !nuevoCorreoElectronico.isBlank()) {
			this.correoElectronico = nuevoCorreoElectronico;
		}
		if (nuevoTelefono != null && !nuevoTelefono.isBlank()) {
			this.telefono = nuevoTelefono;
		}
		if (nuevoRepresentante != null && !nuevoRepresentante.isBlank()) {
			this.representanteLegal = nuevoRepresentante;
		}
	}

	//HU-08: Solicitar ampliación o cambio de rubro
	public void solicitarCambioRubro(Rubro nuevoRubro, String justificacion) {
		if (this.rubro != null && this.rubro.equals(nuevoRubro)) {
			throw new IllegalArgumentException("El nuevo rubro no puede ser igual al actual.");
		}
		// La creación de la SolicitudRadicacion se delega al ServicioEmpresaImpl
	}
}