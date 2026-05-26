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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "censos", uniqueConstraints = {
    @UniqueConstraint(name = "uk_censo_empresa_periodo", columnNames = {"empresa_id", "anio_periodo"})
})
public class Censo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "fecha_declaracion", nullable = false)
    private LocalDate fechaDeclaracion;

    @Column(name = "anio_periodo", nullable = false)
    private int anioPeriodo;

    @Column(name = "cantidad_personal_registrado", nullable = false)
    private int cantidadPersonalRegistrado;

    @Column(name = "cantidad_personal_no_registrado", nullable = false)
    private int cantidadPersonalNoRegistrado;

    @Column(length = 500)
    private String observacion;

    protected Censo() {
    }

    private Censo(
        Empresa empresa,
        int anioPeriodo,
        int cantidadPersonalRegistrado,
        int cantidadPersonalNoRegistrado,
        String observacion
    ) {
        this.empresa = empresa;
        this.fechaDeclaracion = LocalDate.now();
        this.anioPeriodo = anioPeriodo;
        this.cantidadPersonalRegistrado = cantidadPersonalRegistrado;
        this.cantidadPersonalNoRegistrado = cantidadPersonalNoRegistrado;
        this.observacion = observacion;
    }

    public static Censo crear(
        Empresa empresa,
        int anioPeriodo,
        int cantidadPersonalRegistrado,
        int cantidadPersonalNoRegistrado,
        String observacion
    ) {
        return new Censo(empresa, anioPeriodo, cantidadPersonalRegistrado, cantidadPersonalNoRegistrado, observacion);
    }

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public LocalDate getFechaDeclaracion() {
        return fechaDeclaracion;
    }

    public int getAnioPeriodo() {
        return anioPeriodo;
    }

    public int getCantidadPersonalRegistrado() {
        return cantidadPersonalRegistrado;
    }

    public int getCantidadPersonalNoRegistrado() {
        return cantidadPersonalNoRegistrado;
    }

    public String getObservacion() {
        return observacion;
    }

    public int calcularTotalEmpleados() {
        return cantidadPersonalRegistrado + cantidadPersonalNoRegistrado;
    }

    public boolean esPeriodoValida() {
        int anioActual = LocalDate.now().getYear();
        return anioPeriodo >= 2000 && anioPeriodo <= anioActual;
    }

    /** Recibe la flota actual de la empresa y devuelve los vehículos aptos para exención. */
    public List<Vehiculo> obtenerFlotaAptaExencion(List<Vehiculo> flota) {
        return flota.stream()
            .filter(Vehiculo::validarFormatoPatente)
            .toList();
    }
}