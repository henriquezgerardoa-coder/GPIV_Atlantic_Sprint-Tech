package com.gpiv.atlanticsprinttech.entities.dominio;

import java.util.Map;
import java.util.Set;

public enum EstadoRadicacion {
	PENDIENTE,
	EN_REVISION,
	APROBADA,
	RADICADA,
	RECHAZADA,
	REQUIERE_INFORMACION_ADICIONAL,
	CANCELADA;

	private static final Map<EstadoRadicacion, Set<EstadoRadicacion>> TRANSICIONES_VALIDAS = Map.of(
		PENDIENTE, Set.of(EN_REVISION, RECHAZADA, CANCELADA),
		EN_REVISION, Set.of(APROBADA, RECHAZADA, REQUIERE_INFORMACION_ADICIONAL, CANCELADA),
		REQUIERE_INFORMACION_ADICIONAL, Set.of(EN_REVISION, RECHAZADA, CANCELADA),
		APROBADA, Set.of(RADICADA),
		RADICADA, Set.of(),
		RECHAZADA, Set.of(),
		CANCELADA, Set.of()
	);

	public boolean esFinal() {
		return this == RADICADA || this == RECHAZADA || this == CANCELADA;
	}

	public boolean puedeTransicionarA(EstadoRadicacion siguiente) {
		return TRANSICIONES_VALIDAS.getOrDefault(this, Set.of()).contains(siguiente);
	}

	public boolean requiereComentario() {
		return this == RECHAZADA || this == CANCELADA;
	}

	public boolean permiteAsignarLote() {
		return this != RECHAZADA && this != CANCELADA;
	}

	public boolean permiteActaRubrica() {
		return this == APROBADA || this == RADICADA;
	}

	public int etapa() {
		return switch (this) {
			case PENDIENTE, EN_REVISION, REQUIERE_INFORMACION_ADICIONAL -> 2;
			case APROBADA, RADICADA, RECHAZADA, CANCELADA -> 3;
		};
	}
}

