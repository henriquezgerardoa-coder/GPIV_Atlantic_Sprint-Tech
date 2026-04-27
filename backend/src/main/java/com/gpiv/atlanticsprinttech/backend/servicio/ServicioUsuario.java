package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import java.util.Set;

public interface ServicioUsuario {
    List<Usuario> listar();
    Usuario obtenerPorId(Long id);
    Usuario crear(String nombreUsuario, String nombreCompleto, String clavePlano, boolean activo, Set<RolUsuario> roles, Long empresaId);
    Usuario actualizar(Long id, String nombreCompleto, boolean activo, Set<RolUsuario> roles, Long empresaId);
    void eliminar(Long id);
    void restablecerClave(Long id, String claveNueva);
    void cambiarClavePropia(String nombreUsuario, String claveActual, String claveNueva);
    void registrarPublico(String correoElectronico, String clave, String confirmacionClave, String ipCliente);
    void verificarCorreoElectronico(String tokenVerificacion);
    void reenviarCorreoVerificacion(String correoElectronico, String ipCliente);
}