package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajeria_conversaciones")
public class ConversacionMensajeria {

    public static final String ORIGEN_EMPRESA = "EMPRESA";
    public static final String ORIGEN_PUBLICO = "PUBLICO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "empresa_id", nullable = true)
    private Empresa empresa;

    @Column(name = "tipo_origen", nullable = false, length = 20)
    private String tipoOrigen;

    @Column(name = "contacto_nombre_empresa", length = 160)
    private String contactoNombreEmpresa;

    @Column(name = "contacto_correo_electronico", length = 160)
    private String contactoCorreoElectronico;

    @Column(name = "contacto_telefono", length = 40)
    private String contactoTelefono;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_responsable_id", nullable = false)
    private Usuario usuarioResponsable;

    @Column(name = "asunto", nullable = false, length = 160)
    private String asunto;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaUltimaActualizacion;

    protected ConversacionMensajeria() {
    }

    private ConversacionMensajeria(Empresa empresa, Usuario usuarioResponsable, String asunto, String tipoOrigen,
                                   String contactoNombreEmpresa, String contactoCorreoElectronico, String contactoTelefono) {
        this.empresa = empresa;
        this.usuarioResponsable = usuarioResponsable;
        this.asunto = asunto;
        this.tipoOrigen = tipoOrigen;
        this.contactoNombreEmpresa = contactoNombreEmpresa;
        this.contactoCorreoElectronico = contactoCorreoElectronico;
        this.contactoTelefono = contactoTelefono;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaUltimaActualizacion = this.fechaCreacion;
    }

    public static ConversacionMensajeria crear(Empresa empresa, Usuario usuarioResponsable, String asunto) {
        return new ConversacionMensajeria(empresa, usuarioResponsable, asunto, ORIGEN_EMPRESA, null, null, null);
    }

    public static ConversacionMensajeria crearConsultaPublica(
        Usuario usuarioResponsable,
        String asunto,
        String contactoNombreEmpresa,
        String contactoCorreoElectronico,
        String contactoTelefono
    ) {
        return new ConversacionMensajeria(
            null,
            usuarioResponsable,
            asunto,
            ORIGEN_PUBLICO,
            contactoNombreEmpresa,
            contactoCorreoElectronico,
            contactoTelefono
        );
    }

    @PrePersist
    @PreUpdate
    void actualizarMarcaTiempo() {
        this.fechaUltimaActualizacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getTipoOrigen() {
        return tipoOrigen;
    }

    public String getContactoNombreEmpresa() {
        return contactoNombreEmpresa;
    }

    public String getContactoCorreoElectronico() {
        return contactoCorreoElectronico;
    }

    public String getContactoTelefono() {
        return contactoTelefono;
    }

    public Usuario getUsuarioResponsable() {
        return usuarioResponsable;
    }

    public String getAsunto() {
        return asunto;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaUltimaActualizacion() {
        return fechaUltimaActualizacion;
    }

    public void registrarActividad() {
        this.fechaUltimaActualizacion = LocalDateTime.now();
    }

    public boolean esConsultaPublica() {
        return ORIGEN_PUBLICO.equalsIgnoreCase(tipoOrigen);
    }
}

