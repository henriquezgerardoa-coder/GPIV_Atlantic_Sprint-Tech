package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rubros")
public class Rubro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120, unique = true)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "requiere_permiso_especial", nullable = false)
    private boolean requierePermisoEspecial;

    protected Rubro() {
    }

    private Rubro(String nombre, String descripcion, boolean requierePermisoEspecial) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.requierePermisoEspecial = requierePermisoEspecial;
    }

    public static Rubro crear(String nombre, String descripcion, boolean requierePermisoEspecial) {
        return new Rubro(nombre, descripcion, requierePermisoEspecial);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isRequierePermisoEspecial() {
        return requierePermisoEspecial;
    }

    public boolean esCompatibleCon(Rubro otroRubro) {
        if (this.requierePermisoEspecial && otroRubro.requierePermisoEspecial) {
            return false;
        }
        return !this.nombre.equalsIgnoreCase(otroRubro.nombre);
    }

    public void validarCambioRubro(Empresa empresa) {
        if (this.requierePermisoEspecial && !empresa.estaActiva()) {
            throw new IllegalStateException(
                "La empresa debe estar activa para solicitar un rubro con permiso especial"
            );
        }
    }
}
