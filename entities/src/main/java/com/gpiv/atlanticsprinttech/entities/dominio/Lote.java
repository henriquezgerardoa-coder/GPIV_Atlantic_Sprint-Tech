package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Convert;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

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

    @Column(nullable = false)
    private boolean ocupado;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "empresa_id", nullable = true)
    private Empresa empresa;

    @Column(name = "fecha_asignacion")
    private LocalDate fechaAsignacion;

    @Convert(converter = EstadoAsignacionLoteConverter.class)
    @Column(name = "estado_asignacion", length = 40)
    private EstadoAsignacionLote estadoAsignacion;

    @Column(name = "numero_expediente_referencia", length = 40)
    private String numeroExpedienteReferencia;

    @Column(name = "zona", length = 20)
    private String zona;

    protected Lote() {
    }

    private Lote(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Empresa empresa, String zona) {
        this.codigo = codigo;
        this.superficieMetrosCuadrados = superficieMetrosCuadrados;
        this.ocupado = ocupado;
        this.empresa = empresa;
        this.zona = zona;
    }

    public static Lote crear(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Empresa empresa, String zona) {
        return new Lote(codigo, superficieMetrosCuadrados, ocupado, empresa, zona);
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

    public boolean isOcupado() {
        return ocupado;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public LocalDate getFechaAsignacion() {
        return fechaAsignacion;
    }

    public EstadoAsignacionLote getEstadoAsignacion() {
        return estadoAsignacion;
    }

    public String getNumeroExpedienteReferencia() {
        return numeroExpedienteReferencia;
    }

    public String getZona() {
        return zona;
    }

    public boolean esAdjudicable() {
        return !ocupado && empresa == null && estadoAsignacion == null;
    }

    public void actualizarDatos(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Empresa empresa, String zona) {
        this.codigo = codigo;
        this.superficieMetrosCuadrados = superficieMetrosCuadrados;
        this.ocupado = ocupado;
        this.empresa = empresa;
        this.zona = zona;

        if (this.empresa == null) {
            this.fechaAsignacion = null;
            this.estadoAsignacion = null;
            this.numeroExpedienteReferencia = null;
        } else if (this.fechaAsignacion == null) {
            this.fechaAsignacion = LocalDate.now();
        }
    }

    public void actualizarAsignacion(EstadoAsignacionLote estadoAsignacion, String numeroExpedienteReferencia) {
        if (this.empresa == null) {
            this.fechaAsignacion = null;
            this.estadoAsignacion = null;
            this.numeroExpedienteReferencia = null;
            return;
        }

        if (this.fechaAsignacion == null) {
            this.fechaAsignacion = LocalDate.now();
        }
        this.estadoAsignacion = estadoAsignacion;
        this.numeroExpedienteReferencia = numeroExpedienteReferencia;
    }
}

