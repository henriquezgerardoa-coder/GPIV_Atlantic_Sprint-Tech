package com.gpiv.atlanticsprinttech.backend.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.alertas.hitos")
public class PropiedadesAlertasHitos {

    private boolean habilitado = false;
    private String remitente = "no-reply@gpiv.local";
    private int diasAnticipacion = 7;

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public int getDiasAnticipacion() {
        return diasAnticipacion;
    }

    public void setDiasAnticipacion(int diasAnticipacion) {
        this.diasAnticipacion = diasAnticipacion;
    }
}