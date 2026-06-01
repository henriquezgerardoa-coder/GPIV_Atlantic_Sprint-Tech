package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;
import java.util.Set;

public interface ServicioUsuario {
    List<Usuario> listar();
    List<Usuario> listarPorEmpresa(Long empresaId);
    Usuario obtenerPorId(Long id);
    Usuario obtenerPorIdentificador(String identificador);
    Usuario crear(String nombreUsuario, String nombreCompleto, String clavePlano, boolean activo, Set<RolUsuario> roles, Long empresaId);
    Usuario crearComoEmpresa(String nombreUsuario, String nombreCompleto, String clavePlano, Long empresaId, String autorizador);
    Usuario actualizar(Long id, String nombreCompleto, boolean activo, Set<RolUsuario> roles, Long empresaId);
    Usuario desactivar(Long id, String solicitadoPor);
    Usuario activar(Long id, String solicitadoPor);
    void eliminar(Long id);
    void restablecerClave(Long id, String claveNueva);
    void cambiarClavePropia(String nombreUsuario, String claveActual, String claveNueva);
    Usuario actualizarPerfilPropio(String identificadorIngreso, String nombreCompleto, String correoElectronico);
    void registrarPublico(String nombreUsuario, String correoElectronico, String clave, String confirmacionClave, String ipCliente);
    void verificarCorreoElectronico(String tokenVerificacion);
    void reenviarCorreoVerificacion(String correoElectronico, String ipCliente);
    void vincularUsuarioEmpresa(String identificadorIngreso, Long empresaId);
}