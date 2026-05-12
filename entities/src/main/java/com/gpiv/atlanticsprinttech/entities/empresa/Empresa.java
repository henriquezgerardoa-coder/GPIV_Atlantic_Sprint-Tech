package com.gpiv.atlanticsprinttech.entities.empresa;

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
	@Column(name = "fecha_registro", nullable = false)
	private LocalDateTime fechaRegistro;
	@Column(name = "status", nullable = false, length = 30)
	private String estado;
	@Column(name = "cantidad_empleados")
	private Integer cantidadEmpleados;
	@Column(name = "vehiculos_asignados_json", length = 12000)
	private String vehiculosAsignadosJson;
	@Column(name = "representante_legal", length = 120)
	private String representanteLegal;
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
		Integer cantidadEmpleados,
		String vehiculosAsignadosJson
	) {
		this.nombre = nombre;
		this.razonSocial = razonSocial;
		this.nit = nit;
		this.cuit = cuit;
		this.direccion = direccion;
		this.actividadEconomica = actividadEconomica;
		this.correoElectronico = correoElectronico;
		this.telefono = telefono;
		this.fechaRegistro = LocalDateTime.now();
		this.estado = "ACTIVA";
		this.cantidadEmpleados = cantidadEmpleados;
		this.vehiculosAsignadosJson = vehiculosAsignadosJson;
	}

	public static Empresa crear(
		String nombre,
		String razonSocial,
		String nit,
		String cuit,
		String direccion,
		String actividadEconomica,
		String correoElectronico,
		String telefono
	) {
		return new Empresa(
			nombre,
			razonSocial,
			nit,
			cuit,
			direccion,
			actividadEconomica,
			correoElectronico,
			telefono,
			0,
			null
		);
	}

	public static Empresa crear(String nombre, String cuit, String correoElectronico) {
		return new Empresa(nombre, nombre, cuit, cuit, "", "", correoElectronico, null, 0, null);
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

	public String getRazonSocial() {
		return razonSocial;
	}

	public String getNit() {
		return nit;
	}

	public String getDireccion() {
		return direccion;
	}

	public String getActividadEconomica() {
		return actividadEconomica;
	}

	public String getCorreoElectronico() {
		return correoElectronico;
	}

	public String getTelefono() {
		return telefono;
	}

	public LocalDateTime getFechaRegistro() {
		return fechaRegistro;
	}

	public String getEstado() {
		return estado;
	}

	public Integer getCantidadEmpleados() {
		return cantidadEmpleados;
	}

	public int getCantidadEmpleadosOCero() {
		return cantidadEmpleados == null ? 0 : cantidadEmpleados;
	}

	public String getVehiculosAsignadosJson() {
		return vehiculosAsignadosJson;
	}

	public String getRepresentanteLegal() {
		return representanteLegal;
	}

	public Rubro getRubro() {
		return rubro;
	}

	public void actualizarDatos(
		String nombre,
		String razonSocial,
		String nit,
		String cuit,
		String direccion,
		String actividadEconomica,
		String correoElectronico,
		String telefono
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
	//Requerimiento R09
	public void actualizarDatosContacto(
			String nuevoCorreo,
			String nuevoTelefono,
			String nuevoRepresentante
	) {
		if(nuevoCorreo != null && !nuevoCorreo.isBlank())
			this.correoElectronico = nuevoCorreo;
		if(nuevoTelefono != null && !nuevoTelefono.isBlank())
			this.telefono = nuevoTelefono;
		if(nuevoRepresentante != null && !nuevoRepresentante.isBlank())
			this.representanteLegal = nuevoRepresentante;
	}

	public void actualizarServiciosPostRadicacion(Integer cantidadEmpleados, String vehiculosAsignadosJson) {
		this.cantidadEmpleados = cantidadEmpleados;
		this.vehiculosAsignadosJson = vehiculosAsignadosJson;
	}
}