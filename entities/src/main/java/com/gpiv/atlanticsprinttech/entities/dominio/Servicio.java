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

    protected Servicio() {
    }

    private Servicio(String nombre, String descripcionTecnica) {
        this.nombre = nombre;
        this.descripcionTecnica = descripcionTecnica;
        this.estadoActual = EstadoServicio.OPERATIVO;
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

    public void reportarFalla(String comentario, Usuario tecnico) {
        this.estadoActual = EstadoServicio.FALLA_CRITICA;
        this.ultimoComentario = comentario;
        this.ultimoTecnicoResponsable = tecnico;
    }

    public void registrarMantenimiento() {
        this.estadoActual = EstadoServicio.MANTENIMIENTO;
    }

    public void marcarOperativo() {
        this.estadoActual = EstadoServicio.OPERATIVO;
        this.ultimoComentario = null;
    }
}