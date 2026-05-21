package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.AuditLog;
import java.util.List;

public interface ServicioAuditLog {

    /**
     * Persiste un evento de auditoría. Corre en transacción independiente
     * para garantizar el registro incluso si la transacción llamante falla.
     */
    void registrarEvento(
        String usuarioResponsable,
        String operacion,
        String entidadAfectada,
        String idReferencia,
        String valorAnterior,
        String valorNuevo,
        String direccionIp
    );

    List<AuditLog> filtrarPorEntidad(String entidad);

    List<AuditLog> filtrarPorUsuario(String usuario);

    List<AuditLog> listarTodos();
}