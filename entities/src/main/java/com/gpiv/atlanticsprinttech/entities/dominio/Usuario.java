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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
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
    @Column(name = "correo_electronico", length = 160, unique = true)
    private String correoElectronico;
    @Column(name = "clave_acceso_hash", nullable = false, length = 120)
    private String claveAccesoHash;
    @Column(nullable = false)
    private boolean activo;
    @Column(name = "email_verificado")
    private Boolean emailVerificado;
    @Column(name = "token_verificacion_email", length = 120)
    private String tokenVerificacionEmail;
    @Column(name = "token_verificacion_expira_en")
    private LocalDateTime tokenVerificacionExpiraEn;
    @Column(name = "fecha_verificacion_email")
    private LocalDateTime fechaVerificacionEmail;
    @Column(name = "fecha_ultimo_acceso")
    private LocalDateTime fechaUltimoAcceso;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "usuarios_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "rol", nullable = false, length = 40)
    private Set<RolUsuario> roles = new HashSet<>();
    protected Usuario() {
    }
    private Usuario(
        String nombreUsuario,
        String nombreCompleto,
        String correoElectronico,
        String claveAccesoHash,
        boolean activo,
        boolean emailVerificado,
        Set<RolUsuario> roles
    ) {
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correoElectronico = correoElectronico;
        this.claveAccesoHash = claveAccesoHash;
        this.activo = activo;
        this.emailVerificado = emailVerificado;
        this.roles = new HashSet<>(roles);
    }
    public static Usuario crear(String nombreUsuario, String nombreCompleto, String claveAccesoHash, boolean activo,
                                Set<RolUsuario> roles) {
        return new Usuario(nombreUsuario, nombreCompleto, null, claveAccesoHash, activo, true, roles);
    }
    public static Usuario crearPendienteVerificacion(
        String nombreUsuario,
        String nombreCompleto,
        String correoElectronico,
        String claveAccesoHash,
        Set<RolUsuario> roles
    ) {
        return new Usuario(nombreUsuario, nombreCompleto, correoElectronico, claveAccesoHash, true, false, roles);
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
    public String getCorreoElectronico() {
        return correoElectronico;
    }
    public String getClaveAccesoHash() {
        return claveAccesoHash;
    }
    public boolean isActivo() {
        return activo;
    }
    public boolean isEmailVerificado() {
        return Boolean.TRUE.equals(emailVerificado);
    }
    public String getTokenVerificacionEmail() {
        return tokenVerificacionEmail;
    }
    public LocalDateTime getTokenVerificacionExpiraEn() {
        return tokenVerificacionExpiraEn;
    }
    public Set<RolUsuario> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public LocalDateTime getFechaUltimoAcceso() {
        return fechaUltimoAcceso;
    }
    public Empresa getEmpresa() {
        return empresa;
    }
    public Long getEmpresaId() {
        return empresa == null ? null : empresa.getId();
    }
    public void actualizarDatos(String nombreCompleto, boolean activo, Set<RolUsuario> roles, Empresa empresa) {
        this.nombreCompleto = nombreCompleto;
        this.activo = activo;
        this.roles = new HashSet<>(roles);
        this.empresa = empresa;
    }
    public void actualizarClaveAccesoHash(String claveAccesoHash) {
        this.claveAccesoHash = claveAccesoHash;
    }
    public void actualizarCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public void actualizarPerfilPersonal(String nombreCompleto, String correoElectronico) {
        this.nombreCompleto = nombreCompleto;
        this.correoElectronico = correoElectronico;
    }

    public void actualizarEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
    public void iniciarVerificacionEmail(String tokenVerificacionEmail, LocalDateTime tokenVerificacionExpiraEn) {
        this.tokenVerificacionEmail = tokenVerificacionEmail;
        this.tokenVerificacionExpiraEn = tokenVerificacionExpiraEn;
    }
    public boolean tokenVerificacionVigente(LocalDateTime ahora) {
        return tokenVerificacionEmail != null
            && tokenVerificacionExpiraEn != null
            && tokenVerificacionExpiraEn.isAfter(ahora);
    }
    public void marcarEmailVerificado(LocalDateTime fechaVerificacion) {
        this.emailVerificado = Boolean.TRUE;
        this.fechaVerificacionEmail = fechaVerificacion;
        this.tokenVerificacionEmail = null;
        this.tokenVerificacionExpiraEn = null;
    }

    public void registrarUltimoAcceso(LocalDateTime fechaUltimoAcceso) {
        this.fechaUltimoAcceso = fechaUltimoAcceso;
    }

    public boolean tieneRol(RolUsuario rol) {
        return roles.contains(rol);
    }

    public boolean tieneRol(String nombreRol) {
        return roles.stream().anyMatch(r -> r.name().equalsIgnoreCase(nombreRol));
    }

    public void bloquearCuenta() {
        this.activo = false;
    }
}

