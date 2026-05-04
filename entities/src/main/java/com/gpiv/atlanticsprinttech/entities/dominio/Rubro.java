package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "rubros")
public class Rubro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "requiere_permiso_especial", nullable = false)
    private boolean requierePermisoEspecial;

    protected Rubro() {
    }

    private Rubro(String nombre, String descripcion, Boolean requierePermisoEspecial) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.requierePermisoEspecial = requierePermisoEspecial != null ? requierePermisoEspecial : false;
    }

    public static Rubro crear(String nombre, String descripcion, Boolean requierePermisoEspecial) {
        return new Rubro(nombre, descripcion, requierePermisoEspecial);
    }

    // getters
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Boolean getRequierePermisoEspecial() { return requierePermisoEspecial; }

    //metodos

    public boolean isCompatibleCon(Rubro otroRubro) {
        //TODO:tabla intermedia en la base de datos (ej. rubros_incompatibles) para cruzar IDs.
        return true;
    }

     // *parte de esta lógica podría moverse a un Servicio de Dominio, ej RadicacionService.
    public void validarCambioRubro(Empresa empresa) {
        if (empresa == null) {
            throw new IllegalArgumentException("La empresa no puede ser nula.");
        }

        if (empresa.getRubro() != null && empresa.getRubro().getId().equals(this.id)) {
            throw new IllegalStateException("La empresa ya pertenece a este rubro.");
        }

        // si es de alto impacto (ej. Químico)
        if (this.requierePermisoEspecial) {
            //
        }
    }
}