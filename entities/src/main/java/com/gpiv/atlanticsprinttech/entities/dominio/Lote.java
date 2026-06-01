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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;


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

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa", nullable = false, length = 40)
    private EtapaCicloLote etapa = EtapaCicloLote.DISPONIBLE;

    @Column(name = "fecha_inicio_etapa_actual")
    private LocalDate fechaInicioEtapaActual;

    @Column(name = "fecha_limite_etapa_actual")
    private LocalDate fechaLimiteEtapaActual;

    @Column(name = "numero_expediente_referencia", length = 40)
    private String numeroExpedienteReferencia;

    @Column(name = "zona", length = 20)
    private String zona;

    // insertable/updatable=false: JPA no puede bindear geometry como varchar; actualizaciones via query nativa
    @Column(name = "geom", columnDefinition = "geometry(Polygon,4326)", insertable = false, updatable = false)
    private String geom;

    @Column(name = "external_id", length = 100)
    private String externalId;

    // insertable/updatable=false: JPA no puede bindear jsonb como varchar; actualizaciones via query nativa
    @Column(name = "properties", columnDefinition = "jsonb", insertable = false, updatable = false)
    private String properties;

    @Column(name = "color_personalizado", length = 20)
    private String colorPersonalizado;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "parent_lote_id")
    private Lote parentLote;

    @OneToMany(mappedBy = "parentLote", fetch = FetchType.LAZY)
    private Set<Lote> subLotes = new HashSet<>();

    protected Lote() {
    }

    private Lote(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Empresa empresa, String zona) {
        this.codigo = codigo;
        this.superficieMetrosCuadrados = superficieMetrosCuadrados;
        this.ocupado = ocupado;
        this.empresa = empresa;
        this.zona = zona;
        this.etapa = EtapaCicloLote.DISPONIBLE;
        this.fechaInicioEtapaActual = LocalDate.now();
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

    public EtapaCicloLote getEtapa() {
        return etapa;
    }

    public LocalDate getFechaInicioEtapaActual() {
        return fechaInicioEtapaActual;
    }

    public LocalDate getFechaLimiteEtapaActual() {
        return fechaLimiteEtapaActual;
    }

    public String getNumeroExpedienteReferencia() {
        return numeroExpedienteReferencia;
    }

    public String getZona() {
        return zona;
    }

    public Long getEmpresaId() {
        return empresa != null ? empresa.getId() : null;
    }

    public String getNombreEmpresa() {
        return empresa != null ? empresa.getNombre() : null;
    }

    public String getCuitEmpresa() {
        return empresa != null ? empresa.getCuit() : null;
    }

    public String getRubroEmpresa() {
        return empresa != null && empresa.getRubro() != null ? empresa.getRubro().getNombre() : null;
    }

    public String getReferenteEmpresa() {
        return empresa != null ? empresa.getReferente() : null;
    }

    public String getCorreoElectronicoEmpresa() {
        return empresa != null ? empresa.getCorreoElectronico() : null;
    }

    public String getTelefonoEmpresa() {
        return empresa != null ? empresa.getTelefono() : null;
    }

    public String getFechaAsignacionTexto() {
        return fechaAsignacion != null
            ? fechaAsignacion.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }

    public String getNombreEtapa() {
        return etapa != null ? etapa.name() : null;
    }

    public String getEstadoAsignacionLegacy() {
        return etapa != null ? etapa.estadoAsignacionLegacy() : null;
    }

    public boolean esAdjudicable() {
        return etapa == EtapaCicloLote.DISPONIBLE;
    }

    public boolean tieneEmpresaAsignada() {
        return empresa != null;
    }

    public boolean cambioDeEmpresa(Long nuevaEmpresaId) {
        Long idActual = empresa != null ? empresa.getId() : null;
        return idActual == null || !idActual.equals(nuevaEmpresaId);
    }

    public boolean etapaVencida() {
        return fechaLimiteEtapaActual != null
            && LocalDate.now().isAfter(fechaLimiteEtapaActual)
            && !etapa.esTerminal()
            && etapa != EtapaCicloLote.REVERTIDO
            && etapa != EtapaCicloLote.DISPONIBLE;
    }

    public long diasEnEtapaActual() {
        if (fechaInicioEtapaActual == null) return 0;
        return ChronoUnit.DAYS.between(fechaInicioEtapaActual, LocalDate.now());
    }

    public void transicionar(EtapaCicloLote nuevaEtapa) {
        this.etapa = nuevaEtapa;
        this.fechaInicioEtapaActual = LocalDate.now();
        Integer plazo = nuevaEtapa.plazoDiasDefault();
        this.fechaLimiteEtapaActual = (plazo != null) ? LocalDate.now().plusDays(plazo) : null;
    }

    public void ocuparConEmpresa(Empresa empresaAsignada, String numeroRadicado) {
        this.ocupado = true;
        this.empresa = empresaAsignada;
        if (this.fechaAsignacion == null) {
            this.fechaAsignacion = LocalDate.now();
        }
        this.numeroExpedienteReferencia = numeroRadicado;
        transicionar(EtapaCicloLote.PROYECTO_EN_EVALUACION);
    }

    public void actualizarDatos(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Empresa empresa, String zona) {
        this.codigo = codigo;
        this.superficieMetrosCuadrados = superficieMetrosCuadrados;
        this.ocupado = ocupado;
        this.empresa = empresa;
        this.zona = zona;

        if (this.empresa == null) {
            this.fechaAsignacion = null;
            this.numeroExpedienteReferencia = null;
            transicionar(EtapaCicloLote.DISPONIBLE);
        } else if (this.fechaAsignacion == null) {
            this.fechaAsignacion = LocalDate.now();
        }
    }

    // Nuevos getters/setters para geometría y relaciones
    public String getGeom() {
        return geom;
    }

    public void setGeom(String geom) {
        this.geom = geom;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public Lote getParentLote() {
        return parentLote;
    }

    public void setParentLote(Lote parentLote) {
        this.parentLote = parentLote;
    }

    public Set<Lote> getSubLotes() {
        return subLotes;
    }

    public void setSubLotes(Set<Lote> subLotes) {
        this.subLotes = subLotes;
    }

    public boolean esSubdividido() {
        return !subLotes.isEmpty();
    }

    public String getColorPersonalizado() {
        return colorPersonalizado;
    }

    public void asignarColor(String color) {
        this.colorPersonalizado = (color == null || color.isBlank()) ? null : color.trim();
    }
}
