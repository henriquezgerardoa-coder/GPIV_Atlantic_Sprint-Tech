package com.gpiv.atlanticsprinttech.backend.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.api.usuarios")
public class PropiedadesApiUsuarios {
    private Semilla semilla = new Semilla();
    private RolesLegado rolesLegado = new RolesLegado();
    public Semilla getSemilla() {
        return semilla;
    }
    public void setSemilla(Semilla semilla) {
        this.semilla = semilla;
    }
    public RolesLegado getRolesLegado() {
        return rolesLegado;
    }
    public void setRolesLegado(RolesLegado rolesLegado) {
        this.rolesLegado = rolesLegado;
    }
    public static class Semilla {
        private Credencial admin = new Credencial();
        private Credencial directivo = new Credencial();
        private Credencial secretario = new Credencial();
        private Credencial tecnico = new Credencial();
        private Credencial empresa = new Credencial();
        public Credencial getAdmin() {
            return admin;
        }
        public void setAdmin(Credencial admin) {
            this.admin = admin;
        }
        public Credencial getDirectivo() {
            return directivo;
        }
        public void setDirectivo(Credencial directivo) {
            this.directivo = directivo;
        }
        public Credencial getSecretario() {
            return secretario;
        }
        public void setSecretario(Credencial secretario) {
            this.secretario = secretario;
        }
        public Credencial getTecnico() {
            return tecnico;
        }
        public void setTecnico(Credencial tecnico) {
            this.tecnico = tecnico;
        }
        public Credencial getEmpresa() {
            return empresa;
        }
        public void setEmpresa(Credencial empresa) {
            this.empresa = empresa;
        }
    }
    public static class RolesLegado {
        private String deprecation;
        private String sunset;
        private String link;
        public String getDeprecation() {
            return deprecation;
        }
        public void setDeprecation(String deprecation) {
            this.deprecation = deprecation;
        }
        public String getSunset() {
            return sunset;
        }
        public void setSunset(String sunset) {
            this.sunset = sunset;
        }
        public String getLink() {
            return link;
        }
        public void setLink(String link) {
            this.link = link;
        }
    }
    public static class Credencial {
        private String clave;
        public String getClave() {
            return clave;
        }
        public void setClave(String clave) {
            this.clave = clave;
        }
    }
}