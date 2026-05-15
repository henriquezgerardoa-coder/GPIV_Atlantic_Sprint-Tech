package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "sectores")
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String nombre;

    @OneToMany(mappedBy = "sector", fetch = FetchType.LAZY)
    private List<Lote> lotes = new ArrayList<>();

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

    public List<Lote> getLotes() {
        return Collections.unmodifiableList(lotes);
    }

    public double calcularOcupacion() {
        if (lotes.isEmpty()) return 0.0;
        long ocupados = lotes.stream().filter(Lote::isOcupado).count();
        return (double) ocupados / lotes.size() * 100.0;
    }

    public List<Lote> obtenerLotesDisponibles() {
        return lotes.stream()
            .filter(Lote::esAdjudicable)
            .toList();
    }

    public int contarLotesPorSuperficie(double superficie) {
        return (int) lotes.stream()
            .filter(l -> l.getSuperficieMetrosCuadrados() >= superficie)
            .count();
    }

    public void actualizarNombre(String nombre) {
        this.nombre = nombre;
    }
}
