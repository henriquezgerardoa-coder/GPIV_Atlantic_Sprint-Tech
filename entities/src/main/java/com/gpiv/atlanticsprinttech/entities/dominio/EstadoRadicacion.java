package com.gpiv.atlanticsprinttech.entities.dominio;

public enum EstadoRadicacion {
	PENDIENTE,
	EN_REVISION,
	APROBADA,
	RADICADA,
	RECHAZADA,
	REQUIERE_INFORMACION_ADICIONAL,
	CANCELADA;

	public boolean esFinal() {
		return this == RADICADA || this == RECHAZADA || this == CANCELADA;
	}
}

