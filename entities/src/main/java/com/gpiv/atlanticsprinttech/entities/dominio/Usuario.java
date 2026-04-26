package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
    @UniqueConstraint(name = "uk_usuario_nombre_usuario", columnNames = "nombre_usuario")
})
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "nombre_usuario", nullable = false, length = 60, unique = true)
    private String nombreUsuario;
    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;
    @Column(name = "clave_acceso_hash", nullable = false, length = 120)
    private String claveAccesoHash;
    @Column(nullable = false)
    private boolean activo;
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "usuarios_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "rol", nullable = false, length = 40)
    private Set<RolUsuario> roles = new HashSet<>();
    protected Usuario() {
    }
    private Usuario(String nombreUsuario, String nombreCompleto, String claveAccesoHash, boolean activo, Set<RolUsuario> roles) {
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.claveAccesoHash = claveAccesoHash;
        this.activo = activo;
        this.roles = new HashSet<>(roles);
    }
    public static Usuario crear(String nombreUsuario, String nombreCompleto, String claveAccesoHash, boolean activo,
                                Set<RolUsuario> roles) {
        return new Usuario(nombreUsuario, nombreCompleto, claveAccesoHash, activo, roles);
    }
    public Long getId() {
        return id;
    }
    public String getNombreUsuario() {
        return nombreUsuario;
    }
    public String getNombreCompleto() {
        return nombreCompleto;
    }
    public String getClaveAccesoHash() {
        return claveAccesoHash;
    }
    public boolean isActivo() {
        return activo;
    }
    public Set<RolUsuario> getRoles() {
        return Collections.unmodifiableSet(roles);
    }
    public void actualizarDatos(String nombreCompleto, boolean activo, Set<RolUsuario> roles) {
        this.nombreCompleto = nombreCompleto;
        this.activo = activo;
        this.roles = new HashSet<>(roles);
    }
    public void actualizarClaveAccesoHash(String claveAccesoHash) {
        this.claveAccesoHash = claveAccesoHash;
    }
}

