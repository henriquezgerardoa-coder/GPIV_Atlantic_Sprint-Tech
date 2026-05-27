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

    @Column(name = "rubro_anterior_nombre", length = 120)
    private String rubroAnteriorNombre;

    @Column(name = "justificacion", nullable = false, length = 1000)
    private String justificacion;

    @Column(name = "descripcion_otros", length = 300)
    private String descripcionOtros;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCambioRubro estado;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "rubrica_puntaje_impacto_operativo")
    private Integer puntajeImpactoOperativo;

    @Column(name = "rubrica_puntaje_compatibilidad_parque")
    private Integer puntajeCompatibilidadParque;

    @Column(name = "rubrica_puntaje_viabilidad_tecnica")
    private Integer puntajeViabilidadTecnica;

    @Column(name = "rubrica_puntaje_cumplimiento_normativo")
    private Integer puntajeCumplimientoNormativo;

    @Column(name = "rubrica_observaciones", length = 1000)
    private String observacionesRubrica;

    @Column(name = "solicitado_por", nullable = false, length = 80)
    private String solicitadoPor;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "resuelto_por", length = 80)
    private String resueltoPor;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    protected SolicitudCambioRubro() {
    }

    private SolicitudCambioRubro(
        Empresa empresa,
        Rubro rubroSolicitado,
        String rubroAnteriorNombre,
        String justificacion,
        String descripcionOtros,
        String solicitadoPor
    ) {
        this.empresa = empresa;
        this.rubroSolicitado = rubroSolicitado;
        this.rubroAnteriorNombre = rubroAnteriorNombre;
        this.justificacion = justificacion;
        this.descripcionOtros = descripcionOtros;
        this.solicitadoPor = solicitadoPor;
        this.estado = EstadoCambioRubro.PENDIENTE;
        this.fechaSolicitud = LocalDateTime.now();
    }

    public static SolicitudCambioRubro crear(
        Empresa empresa,
        Rubro rubroSolicitado,
        String rubroAnteriorNombre,
        String justificacion,
        String descripcionOtros,
        String solicitadoPor
    ) {
        return new SolicitudCambioRubro(empresa, rubroSolicitado, rubroAnteriorNombre, justificacion, descripcionOtros, solicitadoPor);
    }

    public void validarResoluble() {
        if (this.estado != EstadoCambioRubro.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden resolver solicitudes en estado PENDIENTE");
        }
    }

    public void aprobar(String resueltoPor) {
        this.estado = EstadoCambioRubro.APROBADA;
        this.resueltoPor = resueltoPor;
        this.fechaResolucion = LocalDateTime.now();
    }

    public void rechazar(String resueltoPor, String motivoRechazo) {
        this.estado = EstadoCambioRubro.RECHAZADA;
        this.resueltoPor = resueltoPor;
        this.motivoRechazo = motivoRechazo;
        this.fechaResolucion = LocalDateTime.now();
    }

    public void registrarRubrica(
        Integer puntajeImpactoOperativo,
        Integer puntajeCompatibilidadParque,
        Integer puntajeViabilidadTecnica,
        Integer puntajeCumplimientoNormativo,
        String observacionesRubrica
    ) {
        this.puntajeImpactoOperativo = puntajeImpactoOperativo;
        this.puntajeCompatibilidadParque = puntajeCompatibilidadParque;
        this.puntajeViabilidadTecnica = puntajeViabilidadTecnica;
        this.puntajeCumplimientoNormativo = puntajeCumplimientoNormativo;
        this.observacionesRubrica = observacionesRubrica;
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Rubro getRubroSolicitado() { return rubroSolicitado; }
    public String getRubroAnteriorNombre() { return rubroAnteriorNombre; }
    public String getJustificacion() { return justificacion; }
    public String getDescripcionOtros() { return descripcionOtros; }
    public boolean esRubroOtros() { return "Otros".equalsIgnoreCase(rubroSolicitado.getNombre()); }
    public EstadoCambioRubro getEstado() { return estado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public Integer getPuntajeImpactoOperativo() { return puntajeImpactoOperativo; }
    public Integer getPuntajeCompatibilidadParque() { return puntajeCompatibilidadParque; }
    public Integer getPuntajeViabilidadTecnica() { return puntajeViabilidadTecnica; }
    public Integer getPuntajeCumplimientoNormativo() { return puntajeCumplimientoNormativo; }
    public String getObservacionesRubrica() { return observacionesRubrica; }
    public String getSolicitadoPor() { return solicitadoPor; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public String getResueltoPor() { return resueltoPor; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
}
