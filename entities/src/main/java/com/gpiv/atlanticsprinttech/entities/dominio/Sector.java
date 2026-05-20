package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sectores")
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String nombre;

    protected Sector() {
    }

    private Sector(String nombre) {
        this.nombre = nombre;
    }

    public static Sector crear(String nombre) {
        return new Sector(nombre);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void actualizarNombre(String nombre) {
        this.nombre = nombre;
    }
}
