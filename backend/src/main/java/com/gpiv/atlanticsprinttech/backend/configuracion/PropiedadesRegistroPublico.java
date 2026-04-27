package com.gpiv.atlanticsprinttech.backend.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.registro")
public class PropiedadesRegistroPublico {
    private String urlBaseVerificacion = "http://localhost:8080/verificar.html";
    private String contactoSoporte = "soporte@gpiv.local";
    private Token token = new Token();
    private Mail mail = new Mail();
    private Limites limites = new Limites();

    public String getUrlBaseVerificacion() {
        return urlBaseVerificacion;
    }

    public void setUrlBaseVerificacion(String urlBaseVerificacion) {
        this.urlBaseVerificacion = urlBaseVerificacion;
    }

    public String getContactoSoporte() {
        return contactoSoporte;
    }

    public void setContactoSoporte(String contactoSoporte) {
        this.contactoSoporte = contactoSoporte;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public Limites getLimites() {
        return limites;
    }

    public void setLimites(Limites limites) {
        this.limites = limites;
    }

    public static class Token {
        private long expiracionHoras = 24;

        public long getExpiracionHoras() {
            return expiracionHoras;
        }

        public void setExpiracionHoras(long expiracionHoras) {
            this.expiracionHoras = expiracionHoras;
        }
    }

    public static class Mail {
        private boolean habilitado = false;
        private String remitente = "no-reply@gpiv.local";

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
    }

    public static class Limites {
        private int maxIntentosRegistro = 5;
        private int maxReenviosVerificacion = 5;
        private int maxIntentosIngreso = 5;
        private long ventanaMinutos = 15;

        public int getMaxIntentosRegistro() {
            return maxIntentosRegistro;
        }

        public void setMaxIntentosRegistro(int maxIntentosRegistro) {
            this.maxIntentosRegistro = maxIntentosRegistro;
        }

        public int getMaxReenviosVerificacion() {
            return maxReenviosVerificacion;
        }

        public void setMaxReenviosVerificacion(int maxReenviosVerificacion) {
            this.maxReenviosVerificacion = maxReenviosVerificacion;
        }

        public int getMaxIntentosIngreso() {
            return maxIntentosIngreso;
        }

        public void setMaxIntentosIngreso(int maxIntentosIngreso) {
            this.maxIntentosIngreso = maxIntentosIngreso;
        }

        public long getVentanaMinutos() {
            return ventanaMinutos;
        }

        public void setVentanaMinutos(long ventanaMinutos) {
            this.ventanaMinutos = ventanaMinutos;
        }
    }
}

