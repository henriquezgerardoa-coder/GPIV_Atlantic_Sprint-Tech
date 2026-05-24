package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpleado;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.seguridad.ServicioContextoUsuario;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaEmpleadosCantidad;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.SolicitudEmpleado;
import com.gpiv.atlanticsprinttech.entities.dominio.Empleado;
import com.gpiv.atlanticsprinttech.entities.dominio.Empresa;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas del Servicio de Empleados")
class ServicioEmpleadoImplTest {

	@InjectMocks
	private ServicioEmpleadoImpl servicioEmpleado;

	@Mock
	private RepositorioEmpleado repositorioEmpleado;

	@Mock
	private RepositorioEmpresa repositorioEmpresa;


	@Mock
	private ServicioContextoUsuario servicioContextoUsuario;

	@Mock
	private ServicioAuditLog servicioAuditLog;

	private Usuario usuarioEmpresa;
	private Usuario usuarioAdmin;
	private Empresa empresa;
	private SolicitudEmpleado solicitudEmpleado;

	@BeforeEach
	void setUp() {
		empresa = Empresa.crear("Mi Empresa", "Empresa S.A.", "20123456789",
			"Calle 1", "Comercio", "empresa@test.com", "1234567890");

		usuarioEmpresa = Usuario.crear("usuario_empresa", "Usuario Empresa",
			"hash_clave", true, Set.of(RolUsuario.EMPRESA));

		usuarioAdmin = Usuario.crear("usuario_admin", "Usuario Admin",
			"hash_clave", true, Set.of(RolUsuario.ADMINISTRADOR));

		solicitudEmpleado = new SolicitudEmpleado("20111222333", "Juan González");
	}

	@Test
	@DisplayName("Crear empleado con rol EMPRESA exitosamente")
	void testCrearEmpleadoExitosamente() {
		Long empresaId = 1L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso(anyString()))
			.thenReturn(usuarioEmpresa);
		when(servicioContextoUsuario.esRolEmpresa(any(Usuario.class)))
			.thenReturn(true);
		when(servicioContextoUsuario.obtenerEmpresaIdRequerido(any(Usuario.class)))
			.thenReturn(empresaId);
		when(repositorioEmpresa.findById(empresaId))
			.thenReturn(Optional.of(empresa));
		when(repositorioEmpleado.existsByEmpresa_IdAndCuit(empresaId, solicitudEmpleado.cuit()))
			.thenReturn(false);
		Empleado empleadoCreado = Empleado.crear(solicitudEmpleado.cuit(), solicitudEmpleado.nombre(), empresa);
		when(repositorioEmpleado.save(any(Empleado.class)))
			.thenReturn(empleadoCreado);

		Empleado resultado = servicioEmpleado.crear(empresaId, solicitudEmpleado, "usuario_empresa");

		assertNotNull(resultado);
		assertEquals(solicitudEmpleado.cuit(), resultado.getCuit());
		assertEquals(solicitudEmpleado.nombre(), resultado.getNombre());
	}

	@Test
	@DisplayName("Error al crear empleado con rol que no es EMPRESA")
	void testCrearEmpleadoConRolIncorrecto() {
		Long empresaId = 1L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso(anyString()))
			.thenReturn(usuarioAdmin);

		assertThrows(ResponseStatusException.class,
			() -> servicioEmpleado.crear(empresaId, solicitudEmpleado, "usuario_admin"),
			"Solo los usuarios de EMPRESA pueden crear empleados"
		);
	}

	@Test
	@DisplayName("Error al crear empleado en otra empresa")
	void testCrearEmpleadoEnOtraEmpresa() {
		Long empresaId = 1L;
		Long otraEmpresaId = 2L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso(anyString()))
			.thenReturn(usuarioEmpresa);
		when(servicioContextoUsuario.esRolEmpresa(any(Usuario.class)))
			.thenReturn(true);
		when(servicioContextoUsuario.obtenerEmpresaIdRequerido(any(Usuario.class)))
			.thenReturn(otraEmpresaId);

		assertThrows(ResponseStatusException.class,
			() -> servicioEmpleado.crear(empresaId, solicitudEmpleado, "usuario_empresa"),
			"No puedes crear empleados en otra empresa"
		);
	}

	@Test
	@DisplayName("Error al crear empleado con CUIT duplicado")
	void testCrearEmpleadoConCuitDuplicado() {
		Long empresaId = 1L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso(anyString()))
			.thenReturn(usuarioEmpresa);
		when(servicioContextoUsuario.esRolEmpresa(any(Usuario.class)))
			.thenReturn(true);
		when(servicioContextoUsuario.obtenerEmpresaIdRequerido(any(Usuario.class)))
			.thenReturn(empresaId);
		when(repositorioEmpresa.findById(empresaId))
			.thenReturn(Optional.of(empresa));
		when(repositorioEmpleado.existsByEmpresa_IdAndCuit(empresaId, solicitudEmpleado.cuit()))
			.thenReturn(true);

		assertThrows(ResponseStatusException.class,
			() -> servicioEmpleado.crear(empresaId, solicitudEmpleado, "usuario_empresa"),
			"El CUIT ya está registrado para esta empresa"
		);
	}

	@Test
	@DisplayName("Obtener cantidad de empleados como ADMIN")
	void testObtenerCantidadComoAdmin() {
		Long empresaId = 1L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso(anyString()))
			.thenReturn(usuarioAdmin);
		when(repositorioEmpresa.findById(empresaId))
			.thenReturn(Optional.of(empresa));
		when(repositorioEmpleado.countByEmpresaId(empresaId))
			.thenReturn(5L);

		RespuestaEmpleadosCantidad resultado = servicioEmpleado.obtenerCantidadPorEmpresa(empresaId, "usuario_admin");

		assertNotNull(resultado);
		assertEquals(5L, resultado.cantidadEmpleados());
	}

	@Test
	@DisplayName("Error al obtener cantidad con rol EMPRESA")
	void testObtenerCantidadComoEmpresa() {
		Long empresaId = 1L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso(anyString()))
			.thenReturn(usuarioEmpresa);

		assertThrows(ResponseStatusException.class,
			() -> servicioEmpleado.obtenerCantidadPorEmpresa(empresaId, "usuario_empresa"),
			"Solo ADMIN y DIRECTIVO pueden acceder a este recurso"
		);
	}
}
