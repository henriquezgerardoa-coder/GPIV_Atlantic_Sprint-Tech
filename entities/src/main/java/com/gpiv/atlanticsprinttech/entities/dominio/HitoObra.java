package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "hitos_obra")
public class HitoObra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private ProyectoProductivo proyecto;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(name = "fecha_vencimiento_real")
    private LocalDate fechaVencimientoReal;

    @Column(nullable = false)
    private boolean cumplido;

    protected HitoObra() {
    }

    private HitoObra(ProyectoProductivo proyecto, String descripcion, LocalDate fechaVencimientoReal) {
        this.proyecto = proyecto;
        this.descripcion = descripcion;
        this.fechaVencimientoReal = fechaVencimientoReal;
        this.cumplido = false;
    }

    public static HitoObra crear(ProyectoProductivo proyecto, String descripcion, LocalDate fechaVencimientoReal) {
        return new HitoObra(proyecto, descripcion, fechaVencimientoReal);
    }

    public Long getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDate getFechaVencimientoReal() {
        return fechaVencimientoReal;
    }

    public boolean isCumplido() {
        return cumplido;
    }

    public boolean estaVencido() {
        return !cumplido && fechaVencimientoReal != null && fechaVencimientoReal.isBefore(LocalDate.now());
    }

    public void validarNoCumplido() {
        if (this.cumplido) {
            throw new IllegalStateException("El hito ya fue marcado como cumplido");
        }
    }

    public void marcarComoCumplido() {
        this.cumplido = true;
    }
}
