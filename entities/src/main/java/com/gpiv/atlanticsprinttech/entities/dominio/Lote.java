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

@Entity
@Table(name = "lotes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_lote_empresa_codigo", columnNames = {"empresa_id", "codigo"})
})
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(name = "superficie_m2", nullable = false)
    private Double superficieMetrosCuadrados;

    @Column(nullable = false)
    private boolean ocupado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    protected Lote() {
    }

    private Lote(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Empresa empresa) {
        this.codigo = codigo;
        this.superficieMetrosCuadrados = superficieMetrosCuadrados;
        this.ocupado = ocupado;
        this.empresa = empresa;
    }

    public static Lote crear(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Empresa empresa) {
        return new Lote(codigo, superficieMetrosCuadrados, ocupado, empresa);
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Double getSuperficieMetrosCuadrados() {
        return superficieMetrosCuadrados;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void actualizarDatos(String codigo, Double superficieMetrosCuadrados, boolean ocupado, Empresa empresa) {
        this.codigo = codigo;
        this.superficieMetrosCuadrados = superficieMetrosCuadrados;
        this.ocupado = ocupado;
        this.empresa = empresa;
    }
}

