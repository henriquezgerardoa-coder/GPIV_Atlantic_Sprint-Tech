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

@Entity
@Table(name = "vehiculos")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 15)
    private String patente;

    @Column(name = "marca_modelo", nullable = false, length = 120)
    private String marcaModelo;

    protected Vehiculo() {
    }

    private Vehiculo(Empresa empresa, String patente, String marcaModelo) {
        this.empresa = empresa;
        this.patente = patente.trim().toUpperCase();
        this.marcaModelo = marcaModelo;
    }

    public static Vehiculo crear(Empresa empresa, String patente, String marcaModelo) {
        return new Vehiculo(empresa, patente, marcaModelo);
    }

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getPatente() {
        return patente;
    }

    public String getMarcaModelo() {
        return marcaModelo;
    }

    /** Valida patente argentina vieja (AAA000) o Mercosur (AA000AA). */
    public boolean validarFormatoPatente() {
        if (patente == null) return false;
        String normalizada = patente.replace("-", "").replace(" ", "");
        return normalizada.matches("[A-Z]{3}\\d{3}") || normalizada.matches("[A-Z]{2}\\d{3}[A-Z]{2}");
    }

    /** Lanza excepción si el formato de patente es inválido. */
    public void validarFormatosPatentes() {
        if (!validarFormatoPatente()) {
            throw new IllegalStateException("Formato de patente inválido: " + patente);
        }
    }
}
