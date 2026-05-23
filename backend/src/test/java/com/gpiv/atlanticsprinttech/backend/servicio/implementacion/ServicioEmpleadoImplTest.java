package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpleado;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioEmpresa;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@DisplayName("Pruebas del Servicio de Empleados")
class ServicioEmpleadoImplTest {

	@InjectMocks
	private ServicioEmpleadoImpl servicioEmpleado;

	@Mock
	private RepositorioEmpleado repositorioEmpleado;

	@Mock
	private RepositorioEmpresa repositorioEmpresa;

	@Mock
	private RepositorioUsuario repositorioUsuario;

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

		// Configurar datos de prueba
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
		// Given
		Long empresaId = 1L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso("usuario_empresa"))
			.thenReturn(usuarioEmpresa);
		when(servicioContextoUsuario.esRolEmpresa(usuarioEmpresa))
			.thenReturn(true);
		when(servicioContextoUsuario.obtenerEmpresaIdRequerido(usuarioEmpresa))
			.thenReturn(empresaId);
		when(repositorioEmpresa.findById(empresaId))
			.thenReturn(Optional.of(empresa));
		when(repositorioEmpleado.existsByEmpresa_IdAndCuit(empresaId, solicitudEmpleado.cuit()))
			.thenReturn(false);

		Empleado empleadoCreado = Empleado.crear(solicitudEmpleado.cuit(),
			solicitudEmpleado.nombre(), empresa);
		when(repositorioEmpleado.save(any(Empleado.class)))
			.thenReturn(empleadoCreado);

		// When
		Empleado resultado = servicioEmpleado.crear(empresaId, solicitudEmpleado, "usuario_empresa");

		// Then
		assertNotNull(resultado);
		assertEquals(solicitudEmpleado.cuit(), resultado.getCuit());
		assertEquals(solicitudEmpleado.nombre(), resultado.getNombre());
	}

	@Test
	@DisplayName("Error al crear empleado con rol que no es EMPRESA")
	void testCrearEmpleadoConRolIncorrecto() {
		// Given
		Long empresaId = 1L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso("usuario_admin"))
			.thenReturn(usuarioAdmin);
		when(servicioContextoUsuario.esRolEmpresa(usuarioAdmin))
			.thenReturn(false);

		// When & Then
		assertThrows(ResponseStatusException.class,
			() -> servicioEmpleado.crear(empresaId, solicitudEmpleado, "usuario_admin"),
			"Solo los usuarios de EMPRESA pueden crear empleados"
		);
	}

	@Test
	@DisplayName("Error al crear empleado en otra empresa")
	void testCrearEmpleadoEnOtraEmpresa() {
		// Given
		Long empresaId = 1L;
		Long otraEmpresaId = 2L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso("usuario_empresa"))
			.thenReturn(usuarioEmpresa);
		when(servicioContextoUsuario.esRolEmpresa(usuarioEmpresa))
			.thenReturn(true);
		when(servicioContextoUsuario.obtenerEmpresaIdRequerido(usuarioEmpresa))
			.thenReturn(otraEmpresaId);

		// When & Then
		assertThrows(ResponseStatusException.class,
			() -> servicioEmpleado.crear(empresaId, solicitudEmpleado, "usuario_empresa"),
			"No puedes crear empleados en otra empresa"
		);
	}

	@Test
	@DisplayName("Error al crear empleado con CUIT duplicado")
	void testCrearEmpleadoConCuitDuplicado() {
		// Given
		Long empresaId = 1L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso("usuario_empresa"))
			.thenReturn(usuarioEmpresa);
		when(servicioContextoUsuario.esRolEmpresa(usuarioEmpresa))
			.thenReturn(true);
		when(servicioContextoUsuario.obtenerEmpresaIdRequerido(usuarioEmpresa))
			.thenReturn(empresaId);
		when(repositorioEmpresa.findById(empresaId))
			.thenReturn(Optional.of(empresa));
		when(repositorioEmpleado.existsByEmpresa_IdAndCuit(empresaId, solicitudEmpleado.cuit()))
			.thenReturn(true);

		// When & Then
		assertThrows(ResponseStatusException.class,
			() -> servicioEmpleado.crear(empresaId, solicitudEmpleado, "usuario_empresa"),
			"El CUIT ya está registrado para esta empresa"
		);
	}

	@Test
	@DisplayName("Obtener cantidad de empleados como ADMIN")
	void testObtenerCantidadComoAdmin() {
		// Given
		Long empresaId = 1L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso("usuario_admin"))
			.thenReturn(usuarioAdmin);
		when(servicioContextoUsuario.obtenerEmpresaIdRequerido(usuarioAdmin))
			.thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene empresa"));
		when(repositorioEmpresa.findById(empresaId))
			.thenReturn(Optional.of(empresa));
		when(repositorioEmpleado.countByEmpresaId(empresaId))
			.thenReturn(5L);

		// When
		RespuestaEmpleadosCantidad resultado = servicioEmpleado.obtenerCantidadPorEmpresa(empresaId, "usuario_admin");

		// Then
		assertNotNull(resultado);
		assertEquals(empresaId, resultado.empresaId());
		assertEquals(5L, resultado.cantidadEmpleados());
	}

	@Test
	@DisplayName("Error al obtener cantidad con rol EMPRESA")
	void testObtenerCantidadComoEmpresa() {
		// Given
		Long empresaId = 1L;
		when(servicioContextoUsuario.obtenerUsuarioPorIngreso("usuario_empresa"))
			.thenReturn(usuarioEmpresa);

		// When & Then
		assertThrows(ResponseStatusException.class,
			() -> servicioEmpleado.obtenerCantidadPorEmpresa(empresaId, "usuario_empresa"),
			"Solo ADMIN y DIRECTIVO pueden acceder a este recurso"
		);
	}
}

