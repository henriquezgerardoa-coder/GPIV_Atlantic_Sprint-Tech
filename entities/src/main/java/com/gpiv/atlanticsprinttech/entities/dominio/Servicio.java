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
@Table(name = "servicios")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false, length = 40)
    private EstadoServicio estadoActual;

    @Column(name = "descripcion_tecnica", length = 1000)
    private String descripcionTecnica;

    @Column(name = "ultimo_comentario", length = 500)
    private String ultimoComentario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ultimo_tecnico_id")
    private Usuario ultimoTecnicoResponsable;

    @Column(name = "fecha_ultima_actualizacion")
    private LocalDateTime fechaUltimaActualizacion;

    protected Servicio() {
    }

    private Servicio(String nombre, String descripcionTecnica) {
        this.nombre = nombre;
        this.descripcionTecnica = descripcionTecnica;
        this.estadoActual = EstadoServicio.OPERATIVO;
        this.fechaUltimaActualizacion = LocalDateTime.now();
    }

    public static Servicio crear(String nombre, String descripcionTecnica) {
        return new Servicio(nombre, descripcionTecnica);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public EstadoServicio getEstadoActual() {
        return estadoActual;
    }

    public String getDescripcionTecnica() {
        return descripcionTecnica;
    }

    public String getUltimoComentario() {
        return ultimoComentario;
    }

    public Usuario getUltimoTecnicoResponsable() {
        return ultimoTecnicoResponsable;
    }

    public LocalDateTime getFechaUltimaActualizacion() {
        return fechaUltimaActualizacion;
    }

    public void reportarFalla(String comentario, Usuario tecnico) {
        this.estadoActual = EstadoServicio.FALLA_CRITICA;
        this.ultimoComentario = comentario;
        this.ultimoTecnicoResponsable = tecnico;
        this.fechaUltimaActualizacion = LocalDateTime.now();
    }

    public void registrarMantenimiento(String comentario, Usuario tecnico) {
        this.estadoActual = EstadoServicio.MANTENIMIENTO;
        this.ultimoComentario = comentario;
        this.ultimoTecnicoResponsable = tecnico;
        this.fechaUltimaActualizacion = LocalDateTime.now();
    }

    public void marcarOperativo(String comentario, Usuario tecnico) {
        this.estadoActual = EstadoServicio.OPERATIVO;
        this.ultimoComentario = comentario;
        this.ultimoTecnicoResponsable = tecnico;
        this.fechaUltimaActualizacion = LocalDateTime.now();
    }
}