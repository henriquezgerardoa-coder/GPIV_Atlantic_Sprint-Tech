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

@Entity
@Table(name = "personal_registrado", uniqueConstraints = {
    @UniqueConstraint(name = "uk_personal_empresa_cuit", columnNames = {"empresa_id", "cuit"})
})
public class PersonalRegistrado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 20)
    private String cuit;

    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    protected PersonalRegistrado() {
    }

    private PersonalRegistrado(Empresa empresa, String cuit, String nombreCompleto, LocalDate fechaIngreso) {
        this.empresa = empresa;
        this.cuit = cuit;
        this.nombreCompleto = nombreCompleto;
        this.fechaIngreso = fechaIngreso;
    }

    public static PersonalRegistrado crear(Empresa empresa, String cuit, String nombreCompleto, LocalDate fechaIngreso) {
        return new PersonalRegistrado(empresa, cuit, nombreCompleto, fechaIngreso);
    }

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getCuit() {
        return cuit;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void actualizarDatos(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }
}
