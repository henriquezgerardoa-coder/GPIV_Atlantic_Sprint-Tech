package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioUsuarioImpl implements ServicioUsuario {
    private final RepositorioUsuario repositorioUsuario;
    private final PasswordEncoder codificadorClave;

    public ServicioUsuarioImpl(RepositorioUsuario repositorioUsuario, PasswordEncoder codificadorClave) {
        this.repositorioUsuario = repositorioUsuario;
        this.codificadorClave = codificadorClave;
    }
    @Override
    public List<Usuario> listar() {
        return repositorioUsuario.findAll();
    }
    @Override
    public Usuario obtenerPorId(Long id) {
        return repositorioUsuario.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
    @Override
    public Usuario crear(String nombreUsuario, String nombreCompleto, String clavePlano, boolean activo, Set<RolUsuario> roles) {
        validarRoles(roles);
        if (repositorioUsuario.existsByNombreUsuario(nombreUsuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre");
        }
        Usuario usuario = Usuario.crear(
            nombreUsuario,
            nombreCompleto,
            codificadorClave.encode(clavePlano),
            activo,
            roles
        );
        return repositorioUsuario.save(usuario);
    }
    @Override
    public Usuario actualizar(Long id, String nombreCompleto, boolean activo, Set<RolUsuario> roles) {
        validarRoles(roles);
        Usuario usuario = obtenerPorId(id);
        usuario.actualizarDatos(nombreCompleto, activo, roles);
        return repositorioUsuario.save(usuario);
    }
    @Override
    public void eliminar(Long id) {
        Usuario usuario = obtenerPorId(id);
        repositorioUsuario.delete(usuario);
    }
    @Override
    public void restablecerClave(Long id, String claveNueva) {
        Usuario usuario = obtenerPorId(id);
        usuario.actualizarClaveAccesoHash(codificadorClave.encode(claveNueva));
        repositorioUsuario.save(usuario);
    }
    @Override
    public void cambiarClavePropia(String nombreUsuario, String claveActual, String claveNueva) {
        Usuario usuario = repositorioUsuario.findByNombreUsuarioAndActivoTrue(nombreUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!codificadorClave.matches(claveActual, usuario.getClaveAccesoHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave actual es incorrecta");
        }
        usuario.actualizarClaveAccesoHash(codificadorClave.encode(claveNueva));
        repositorioUsuario.save(usuario);
    }
    private void validarRoles(Set<RolUsuario> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe asignar al menos un rol");
        }
    }
}