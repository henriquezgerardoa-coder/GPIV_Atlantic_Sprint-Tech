package com.gpiv.atlanticsprinttech.entities.empresa;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_cambio_rubro")
public class SolicitudCambioRubro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubro_solicitado_id", nullable = false)
    private Rubro rubroSolicitado;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificacion;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitudCambio estado;

    protected SolicitudCambioRubro() {
    }

    private SolicitudCambioRubro(Empresa empresa, Rubro rubroSolicitado, String justificacion) {
        this.empresa = empresa;
        this.rubroSolicitado = rubroSolicitado;
        this.justificacion = justificacion;
        this.fechaSolicitud = LocalDateTime.now();
        this.estado = EstadoSolicitudCambio.PENDIENTE;
    }

    public static SolicitudCambioRubro crear(Empresa empresa, Rubro rubroSolicitado, String justificacion) {
        return new SolicitudCambioRubro(empresa, rubroSolicitado, justificacion);
    }

    // Getters
    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Rubro getRubroSolicitado() { return rubroSolicitado; }
    public String getJustificacion() { return justificacion; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public EstadoSolicitudCambio getEstado() { return estado; }

    // Metodos para cambiar el estado (Admin)
    public void aprobar() { this.estado = EstadoSolicitudCambio.APROBADA; }
    public void rechazar() { this.estado = EstadoSolicitudCambio.RECHAZADA; }
}