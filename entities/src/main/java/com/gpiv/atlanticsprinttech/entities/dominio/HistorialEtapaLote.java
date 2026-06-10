package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lote_historial_etapa")
public class HistorialEtapaLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_anterior", length = 40)
    private EtapaCicloLote etapaAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_nueva", nullable = false, length = 40)
    private EtapaCicloLote etapaNueva;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    @Column(name = "usuario_responsable", nullable = false, length = 120)
    private String usuarioResponsable;

    @Column(name = "motivo", length = 2000)
    private String motivo;

    @Column(name = "fecha_limite_plazo")
    private LocalDate fechaLimitePlazo;

    protected HistorialEtapaLote() {
    }

    private HistorialEtapaLote(
        Lote lote,
        EtapaCicloLote etapaAnterior,
        EtapaCicloLote etapaNueva,
        String usuarioResponsable,
        String motivo,
        LocalDate fechaLimitePlazo
    ) {
        this.lote = lote;
        this.etapaAnterior = etapaAnterior;
        this.etapaNueva = etapaNueva;
        this.fechaCambio = LocalDateTime.now();
        this.usuarioResponsable = usuarioResponsable;
        this.motivo = motivo;
        this.fechaLimitePlazo = fechaLimitePlazo;
    }

    public static HistorialEtapaLote registrar(
        Lote lote,
        EtapaCicloLote etapaAnterior,
        EtapaCicloLote etapaNueva,
        String usuarioResponsable,
        String motivo,
        LocalDate fechaLimitePlazo
    ) {
        return new HistorialEtapaLote(lote, etapaAnterior, etapaNueva, usuarioResponsable, motivo, fechaLimitePlazo);
    }

    public Long getId() { return id; }
    public Lote getLote() { return lote; }
    public EtapaCicloLote getEtapaAnterior() { return etapaAnterior; }
    public EtapaCicloLote getEtapaNueva() { return etapaNueva; }
    public LocalDateTime getFechaCambio() { return fechaCambio; }
    public String getUsuarioResponsable() { return usuarioResponsable; }
    public String getMotivo() { return motivo; }
    public LocalDate getFechaLimitePlazo() { return fechaLimitePlazo; }
}
