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
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajeria_mensajes")
public class MensajeMensajeria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private ConversacionMensajeria conversacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "usuario_emisor_id", nullable = true)
    private Usuario usuarioEmisor;

    @Column(name = "emisor_externo_nombre", length = 160)
    private String emisorExternoNombre;

    @Column(name = "contenido", nullable = false, length = 4000)
    private String contenido;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    protected MensajeMensajeria() {
    }

    private MensajeMensajeria(ConversacionMensajeria conversacion, Usuario usuarioEmisor, String emisorExternoNombre, String contenido) {
        this.conversacion = conversacion;
        this.usuarioEmisor = usuarioEmisor;
        this.emisorExternoNombre = emisorExternoNombre;
        this.contenido = contenido;
        this.fechaEnvio = LocalDateTime.now();
    }

    public static MensajeMensajeria crear(ConversacionMensajeria conversacion, Usuario usuarioEmisor, String contenido) {
        return new MensajeMensajeria(conversacion, usuarioEmisor, null, contenido);
    }

    public static MensajeMensajeria crearExterno(ConversacionMensajeria conversacion, String emisorExternoNombre, String contenido) {
        return new MensajeMensajeria(conversacion, null, emisorExternoNombre, contenido);
    }

    @PrePersist
    void actualizarMarcaTiempo() {
        this.fechaEnvio = this.fechaEnvio == null ? LocalDateTime.now() : this.fechaEnvio;
    }

    public Long getId() {
        return id;
    }

    public ConversacionMensajeria getConversacion() {
        return conversacion;
    }

    public Usuario getUsuarioEmisor() {
        return usuarioEmisor;
    }

    public String getEmisorExternoNombre() {
        return emisorExternoNombre;
    }

    public String getContenido() {
        return contenido;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public boolean esEmisorExterno() {
        return usuarioEmisor == null;
    }

    public Long getEmisorId() {
        return usuarioEmisor != null ? usuarioEmisor.getId() : null;
    }

    public String getEmisorNombreUsuario() {
        return usuarioEmisor != null ? usuarioEmisor.getNombreUsuario() : null;
    }

    public String getEmisorNombreCompleto() {
        return usuarioEmisor != null ? usuarioEmisor.getNombreCompleto() : null;
    }

    public String getFechaEnvioTexto() {
        return fechaEnvio != null ? fechaEnvio.toString() : null;
    }
}

