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

    @Column(name = "ultima_notificacion_enviada")
    private LocalDate ultimaNotificacionEnviada;

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

    public ProyectoProductivo getProyecto() {
        return proyecto;
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

    public boolean proximoAVencer(int diasUmbral) {
        if (cumplido || fechaVencimientoReal == null) return false;
        LocalDate hoy = LocalDate.now();
        return !fechaVencimientoReal.isBefore(hoy) && !fechaVencimientoReal.isAfter(hoy.plusDays(diasUmbral));
    }

    public boolean necesitaNotificacion() {
        return ultimaNotificacionEnviada == null || ultimaNotificacionEnviada.isBefore(LocalDate.now());
    }

    public void registrarNotificacion() {
        this.ultimaNotificacionEnviada = LocalDate.now();
    }

    public LocalDate getUltimaNotificacionEnviada() {
        return ultimaNotificacionEnviada;
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
