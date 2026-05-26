package com.gpiv.atlanticsprinttech.backend.servicio.seguridad;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
		Usuario usuario = repositorioUsuario.findByNombreUsuarioConEmpresa(identificadorIngreso)
			.or(() -> repositorioUsuario.findByCorreoElectronicoIgnoreCase(normalizado))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
		usuario.registrarUltimoAcceso(LocalDateTime.now());
		repositorioUsuario.save(usuario);
		return usuario;
	}

	public boolean esRolEmpresa(Usuario usuario) {
		return usuario.tieneRol(RolUsuario.EMPRESA);
	}

	public boolean esRolAdministrador(Usuario usuario) {
		return usuario.tieneRol(RolUsuario.ADMINISTRADOR);
	}

	public boolean esRolDirectivo(Usuario usuario) {
		return usuario.tieneRol(RolUsuario.DIRECTIVO);
	}

	public boolean esRolTecnico(Usuario usuario) {
		return usuario.tieneRol(RolUsuario.TECNICO);
	}

	public Long obtenerEmpresaIdRequerido(Usuario usuario) {
		if (usuario.getEmpresaId() == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario EMPRESA no tiene una empresa asociada");
		}
		return usuario.getEmpresaId();
	}

	public String obtenerIdentificadorActual() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return (auth != null && auth.isAuthenticated()) ? auth.getName() : "sistema";
	}

	public void exigirAccesoAEmpresa(Usuario usuario, Long empresaId) {
		if (esRolEmpresa(usuario)) {
			Long propiaEmpresaId = obtenerEmpresaIdRequerido(usuario);
			if (!propiaEmpresaId.equals(empresaId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes acceder a recursos de otra empresa");
			}
		}
	}
}

