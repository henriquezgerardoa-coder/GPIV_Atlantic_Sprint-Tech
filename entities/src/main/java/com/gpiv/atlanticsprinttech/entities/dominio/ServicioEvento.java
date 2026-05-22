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
@Table(name = "servicio_eventos")
public class ServicioEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoServicio estado;

    @Column(length = 500)
    private String comentario;

    @Column(nullable = false, length = 80)
    private String usuario;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    protected ServicioEvento() {
    }

    private ServicioEvento(Servicio servicio, EstadoServicio estado, String comentario, String usuario) {
        this.servicio = servicio;
        this.estado = estado;
        this.comentario = comentario;
        this.usuario = usuario;
        this.fechaEvento = LocalDateTime.now();
    }

    public static ServicioEvento crear(Servicio servicio, EstadoServicio estado, String comentario, String usuario) {
        return new ServicioEvento(servicio, estado, comentario, usuario);
    }

    public Long getId() { return id; }
    public Servicio getServicio() { return servicio; }
    public EstadoServicio getEstado() { return estado; }
    public String getComentario() { return comentario; }
    public String getUsuario() { return usuario; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
}
