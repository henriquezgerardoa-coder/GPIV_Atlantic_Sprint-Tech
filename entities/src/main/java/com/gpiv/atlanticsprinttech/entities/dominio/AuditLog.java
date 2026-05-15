package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad de solo lectura (inmutable): captura quién, cuándo y qué de cada cambio crítico.
 * Nunca se modifica tras su creación. Persístase vía RepositorioAuditLog.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "usuario_responsable", nullable = false, length = 120)
    private String usuarioResponsable;

    @Column(nullable = false, length = 80)
    private String operacion;

    @Column(name = "entidad_afectada", nullable = false, length = 80)
    private String entidadAfectada;

    @Column(name = "id_referencia", length = 40)
    private String idReferencia;

    @Column(name = "valor_anterior", length = 2000)
    private String valorAnterior;

    @Column(name = "valor_nuevo", length = 2000)
    private String valorNuevo;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    protected AuditLog() {
    }

    private AuditLog(
        String usuarioResponsable,
        String operacion,
        String entidadAfectada,
        String idReferencia,
        String valorAnterior,
        String valorNuevo,
        String direccionIp
    ) {
        this.fechaHora = LocalDateTime.now();
        this.usuarioResponsable = usuarioResponsable;
        this.operacion = operacion;
        this.entidadAfectada = entidadAfectada;
        this.idReferencia = idReferencia;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.direccionIp = direccionIp;
    }

    /** Invocado automáticamente por otros módulos al cambiar estados críticos. Persistir con RepositorioAuditLog. */
    public static AuditLog registrarEvento(
        String usuarioResponsable,
        String operacion,
        String entidadAfectada,
        String idReferencia,
        String valorAnterior,
        String valorNuevo,
        String direccionIp
    ) {
        return new AuditLog(usuarioResponsable, operacion, entidadAfectada,
            idReferencia, valorAnterior, valorNuevo, direccionIp);
    }

    /** Filtra en memoria; para consultas DB usar RepositorioAuditLog. */
    public static List<AuditLog> filtrarPorEntidad(List<AuditLog> logs, String entidad) {
        return logs.stream()
            .filter(l -> entidad.equals(l.entidadAfectada))
            .toList();
    }

    /** Filtra en memoria; para consultas DB usar RepositorioAuditLog. */
    public static List<AuditLog> filtrarPorUsuario(List<AuditLog> logs, String usuario) {
        return logs.stream()
            .filter(l -> usuario.equals(l.usuarioResponsable))
            .toList();
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getUsuarioResponsable() {
        return usuarioResponsable;
    }

    public String getOperacion() {
        return operacion;
    }

    public String getEntidadAfectada() {
        return entidadAfectada;
    }

    public String getIdReferencia() {
        return idReferencia;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public String generarReporteInmutable() {
        return String.format("[%s] %s | Usuario: %s | Entidad: %s (ref: %s) | %s → %s | IP: %s",
            fechaHora, operacion, usuarioResponsable, entidadAfectada, idReferencia,
            valorAnterior, valorNuevo, direccionIp);
    }
}
