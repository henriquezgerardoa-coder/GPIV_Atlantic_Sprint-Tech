package com.gpiv.atlanticsprinttech.entities.dominio;

public enum EstadoProyecto {
    PLANIFICADO,
    EN_EJECUCION,
    PAUSADO,
    FINALIZADO,
    CANCELADO;

    public boolean esFinal() {
        return this == FINALIZADO || this == CANCELADO;
    }
}
