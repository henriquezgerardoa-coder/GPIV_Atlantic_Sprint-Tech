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
@Table(name = "radicacion_historial")
public class RadicacionHistorial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "radicacion_id", nullable = false)
    private RadicacionSolicitud radicacion;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 40)
    private EstadoRadicacion estadoAnterior;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoRadicacion estado;
    @Column(name = "comentario", length = 1000)
    private String comentario;
    @Column(name = "usuario", nullable = false, length = 80)
    private String usuario;
    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;
    protected RadicacionHistorial() {
    }
    private RadicacionHistorial(RadicacionSolicitud radicacion, EstadoRadicacion estadoAnterior, EstadoRadicacion estado, String comentario, String usuario) {
        this.radicacion = radicacion;
        this.estadoAnterior = estadoAnterior;
        this.estado = estado;
        this.comentario = comentario;
        this.usuario = usuario;
        this.fechaEvento = LocalDateTime.now();
    }
    public static RadicacionHistorial crear(RadicacionSolicitud radicacion, EstadoRadicacion estadoAnterior, EstadoRadicacion estado, String comentario, String usuario) {
        return new RadicacionHistorial(radicacion, estadoAnterior, estado, comentario, usuario);
    }
    public Long getId() {
        return id;
    }
    public EstadoRadicacion getEstadoAnterior() {
        return estadoAnterior;
    }
    public EstadoRadicacion getEstado() {
        return estado;
    }
    public String getComentario() {
        return comentario;
    }
    public String getUsuario() {
        return usuario;
    }
    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }
}