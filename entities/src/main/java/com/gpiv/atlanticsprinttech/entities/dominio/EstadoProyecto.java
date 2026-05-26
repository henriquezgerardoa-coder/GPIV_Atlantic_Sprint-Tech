package com.gpiv.atlanticsprinttech.entities.dominio;

public enum EstadoProyecto {
    INICIADO,
    EN_EJECUCION,
    DETENIDO,
    COMPLETADO,
    CANCELADO;

    public boolean esFinal() {
        return this == COMPLETADO || this == CANCELADO;
    }
}