package com.gpiv.atlanticsprinttech.backend.servicio.seguridad;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ServicioContextoUsuario {

	private final RepositorioUsuario repositorioUsuario;

	public ServicioContextoUsuario(RepositorioUsuario repositorioUsuario) {
		this.repositorioUsuario = repositorioUsuario;
	}

	public Usuario obtenerUsuarioPorIngreso(String identificadorIngreso) {
		String normalizado = identificadorIngreso == null ? "" : identificadorIngreso.trim().toLowerCase(Locale.ROOT);
		return repositorioUsuario.findByNombreUsuarioConEmpresa(identificadorIngreso)
			.or(() -> repositorioUsuario.findByCorreoElectronicoIgnoreCase(normalizado))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
	}

	public boolean esRolEmpresa(Usuario usuario) {
		return usuario.getRoles().contains(RolUsuario.EMPRESA);
	}

	public Long obtenerEmpresaIdRequerido(Usuario usuario) {
		if (usuario.getEmpresaId() == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario EMPRESA no tiene una empresa asociada");
		}
		return usuario.getEmpresaId();
	}
}

