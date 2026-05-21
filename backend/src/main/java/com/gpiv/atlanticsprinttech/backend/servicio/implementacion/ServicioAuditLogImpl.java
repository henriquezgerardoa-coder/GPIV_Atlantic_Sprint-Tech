package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.entities.dominio.AuditLog;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioAuditLogImpl implements ServicioAuditLog {

    private final RepositorioAuditLog repositorioAuditLog;

    public ServicioAuditLogImpl(RepositorioAuditLog repositorioAuditLog) {
        this.repositorioAuditLog = repositorioAuditLog;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEvento(
        String usuarioResponsable,
        String operacion,
        String entidadAfectada,
        String idReferencia,
        String valorAnterior,
        String valorNuevo,
        String direccionIp
    ) {
        repositorioAuditLog.save(
            AuditLog.registrarEvento(
                usuarioResponsable, operacion, entidadAfectada,
                idReferencia, valorAnterior, valorNuevo, direccionIp
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> filtrarPorEntidad(String entidad) {
        return repositorioAuditLog.findByEntidadAfectadaOrderByFechaHoraDesc(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> filtrarPorUsuario(String usuario) {
        return repositorioAuditLog.findByUsuarioResponsableOrderByFechaHoraDesc(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> listarTodos() {
        return repositorioAuditLog.findAllByOrderByFechaHoraDesc();
    }
}