package com.gpiv.atlanticsprinttech.entities.lote;

import com.gpiv.atlanticsprinttech.entities.compartido.ExcepcionNegocio;
import com.gpiv.atlanticsprinttech.entities.empresa.Empresa;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "lotes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_lote_empresa_codigo", columnNames = {"empresa_id", "codigo"})
})
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(name = "superficie_m2", nullable = false)
    private Double superficieMetrosCuadrados;

    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon poligono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "fecha_asignacion")
    private LocalDate fechaAsignacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_asignacion", length = 40, nullable = false)
    private EstadoAsignacionLote estadoAsignacion;

    @Column(name = "numero_expediente_referencia", length = 40)
    private String numeroExpedienteReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "zona", length = 20)
    private ZonaParque zona;

    @Column(name = "ocupado", nullable = false)
    private boolean ocupado;

    @SuppressWarnings("unused") // Requerido por JPA
    protected Lote() {}

    private Lote(String codigo, Double superficieMetrosCuadrados, ZonaParque zona) {
        this.codigo = codigo;
        this.superficieMetrosCuadrados = superficieMetrosCuadrados;
        this.estadoAsignacion = EstadoAsignacionLote.DISPONIBLE;
        this.ocupado = false;
        this.zona = zona;
    }

    public static Lote crear(String codigo, Double superficieMetrosCuadrados, ZonaParque zona) {
        return new Lote(codigo, superficieMetrosCuadrados, zona);
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Double getSuperficieMetrosCuadrados() {
        return superficieMetrosCuadrados;
    }

    public boolean estaOcupado() {
        return ocupado;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public LocalDate getFechaAsignacion() {
        return fechaAsignacion;
    }

    public EstadoAsignacionLote getEstadoAsignacion() { return estadoAsignacion; }

    /**
     * TDA: El lote sabe su propio empresaId sin que el servicio tenga que preguntar
     * por la empresa y deferenciar. Equivalente al patrón ya aplicado en Usuario.
     */
    public Long getEmpresaId() {
        return empresa == null ? null : empresa.getId();
    }

    public String getNumeroExpedienteReferencia() {
        return numeroExpedienteReferencia;
    }

    public ZonaParque getZona() {
        return zona;
    }

    public boolean esAdjudicable() {
        return this.estadoAsignacion == EstadoAsignacionLote.DISPONIBLE;
    }

    public void actualizarDatos(String codigo, Double superficieMetrosCuadrados, ZonaParque zona) {
        this.codigo = codigo;
        this.superficieMetrosCuadrados = superficieMetrosCuadrados;
        this.zona = zona;
    }

    public void preAdjudicarALaSolicitud(String numeroExpediente) throws ExcepcionNegocio {
        if (this.estadoAsignacion != EstadoAsignacionLote.DISPONIBLE) {
            throw new ExcepcionNegocio("El lote " + this.codigo + " no está disponible para pre-adjudicación. Estado actual: " + this.estadoAsignacion);
        }
        this.estadoAsignacion = EstadoAsignacionLote.PREADJUDICADO_A_SOLICITUD;
        this.ocupado = true;
        this.empresa = null;
        this.numeroExpedienteReferencia = numeroExpediente;
        this.fechaAsignacion = LocalDate.now();
    }

    public void asignarEmpresa(Empresa nuevaEmpresa, String expediente) throws ExcepcionNegocio {
        if (nuevaEmpresa == null) {
            throw new ExcepcionNegocio("No se puede asignar un lote a una empresa nula.");
        }
        if (this.estadoAsignacion != EstadoAsignacionLote.PREADJUDICADO_A_SOLICITUD
            && this.estadoAsignacion != EstadoAsignacionLote.DISPONIBLE
            && this.estadoAsignacion != EstadoAsignacionLote.ASIGNADO_A_EMPRESA) {
            throw new ExcepcionNegocio("El lote " + this.codigo + " no puede ser asignado en su estado actual. Estado actual: " + this.estadoAsignacion);
        }

        this.empresa = nuevaEmpresa;
        this.estadoAsignacion = EstadoAsignacionLote.ASIGNADO_A_EMPRESA;
        this.ocupado = true;
        this.numeroExpedienteReferencia = expediente;
        this.fechaAsignacion = LocalDate.now();
    }

    public void adjudicarRadicada(Empresa nuevaEmpresa, String expediente) throws ExcepcionNegocio {
        if (nuevaEmpresa == null) {
            throw new ExcepcionNegocio("No se puede adjudicar un lote a una empresa nula.");
        }
        this.empresa = nuevaEmpresa;
        this.estadoAsignacion = EstadoAsignacionLote.ADJUDICADO_RADICADA;
        this.ocupado = true;
        this.numeroExpedienteReferencia = expediente;
        this.fechaAsignacion = LocalDate.now();
    }

    public void liberar() {
        this.empresa = null;
        this.estadoAsignacion = EstadoAsignacionLote.DISPONIBLE;
        this.ocupado = false;
        this.numeroExpedienteReferencia = null;
        this.fechaAsignacion = null;
    }

    public Polygon getPoligono() {
        return poligono;
    }

    @SuppressWarnings("unused") // Disponible para endpoint de actualizacion de polígono
    public void setPoligono(Polygon poligono) {
        this.poligono = poligono;
    }
}
