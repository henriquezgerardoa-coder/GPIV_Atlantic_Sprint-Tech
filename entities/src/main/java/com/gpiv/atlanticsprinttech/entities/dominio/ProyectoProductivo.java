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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "proyectos_productivos")
public class ProyectoProductivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoProyecto estado;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(name = "fecha_inicio_real")
    private LocalDate fechaInicioReal;

    @Column(name = "fecha_estimada_fin")
    private LocalDate fechaEstimadaFin;

    @Column(name = "monto_inversion", precision = 15, scale = 2)
    private BigDecimal montoInversion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsableSeguimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_origen_id")
    private RadicacionSolicitud solicitudOrigen;

    @OneToMany(mappedBy = "proyecto", fetch = FetchType.LAZY)
    private List<HitoObra> hitos = new ArrayList<>();

    @OneToMany(mappedBy = "proyecto", fetch = FetchType.LAZY)
    private List<DocumentoPDF> documentos = new ArrayList<>();

    protected ProyectoProductivo() {
    }

    private ProyectoProductivo(
        String nombre,
        String descripcion,
        BigDecimal montoInversion,
        LocalDate fechaEstimadaFin,
        Usuario responsableSeguimiento
    ) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.montoInversion = montoInversion;
        this.fechaEstimadaFin = fechaEstimadaFin;
        this.responsableSeguimiento = responsableSeguimiento;
        this.estado = EstadoProyecto.PLANIFICADO;
        this.fechaCreacion = LocalDateTime.now();
    }

    public static ProyectoProductivo crear(
        String nombre,
        String descripcion,
        BigDecimal montoInversion,
        LocalDate fechaEstimadaFin,
        Usuario responsableSeguimiento
    ) {
        return new ProyectoProductivo(nombre, descripcion, montoInversion, fechaEstimadaFin, responsableSeguimiento);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public EstadoProyecto getEstado() {
        return estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDate getFechaInicioReal() {
        return fechaInicioReal;
    }

    public LocalDate getFechaEstimadaFin() {
        return fechaEstimadaFin;
    }

    public BigDecimal getMontoInversion() {
        return montoInversion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Usuario getResponsableSeguimiento() {
        return responsableSeguimiento;
    }

    public RadicacionSolicitud getSolicitudOrigen() {
        return solicitudOrigen;
    }

    public List<HitoObra> getHitos() {
        return Collections.unmodifiableList(hitos);
    }

    public List<DocumentoPDF> getDocumentos() {
        return Collections.unmodifiableList(documentos);
    }

    public void iniciarProyecto(RadicacionSolicitud solicitud) {
        if (this.estado != EstadoProyecto.PLANIFICADO) {
            throw new IllegalStateException("El proyecto ya fue iniciado");
        }
        this.solicitudOrigen = solicitud;
        this.fechaInicioReal = LocalDate.now();
        this.estado = EstadoProyecto.EN_EJECUCION;
    }

    public void actualizarEstado(EstadoProyecto nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public double calcularAvanceFisico() {
        if (hitos.isEmpty()) return 0.0;
        long cumplidos = hitos.stream().filter(HitoObra::isCumplido).count();
        return (double) cumplidos / hitos.size() * 100.0;
    }

    public void registrarDocumentacionTecnica(DocumentoPDF doc) {
        documentos.add(doc);
    }

    public boolean validarVencimientoPlazo() {
        return hitos.stream().anyMatch(HitoObra::estaVencido);
    }
}