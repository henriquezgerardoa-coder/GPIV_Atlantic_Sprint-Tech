package com.gpiv.atlanticsprinttech.backend.repositorio;

import com.gpiv.atlanticsprinttech.entities.dominio.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioAuditLog extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntidadAfectadaOrderByFechaHoraDesc(String entidadAfectada);
    List<AuditLog> findByUsuarioResponsableOrderByFechaHoraDesc(String usuarioResponsable);
    List<AuditLog> findAllByOrderByFechaHoraDesc();
}