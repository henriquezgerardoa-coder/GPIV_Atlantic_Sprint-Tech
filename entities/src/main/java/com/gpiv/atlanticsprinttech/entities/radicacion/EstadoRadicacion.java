package com.gpiv.atlanticsprinttech.entities.radicacion;

import java.util.Set;

public enum EstadoRadicacion {
	CANCELADA,
	RECHAZADA,
	RADICADA,
	APROBADA {
		@Override
		public Set<EstadoRadicacion> estadosPermitidos() {
			return Set.of(RADICADA, CANCELADA);
		}
	},
	REQUIERE_INFORMACION_ADICIONAL {
		@Override
		public Set<EstadoRadicacion> estadosPermitidos() {
			return Set.of(EN_REVISION, CANCELADA);
		}
	},
	EN_REVISION {
		@Override
		public Set<EstadoRadicacion> estadosPermitidos() {
			return Set.of(APROBADA, RECHAZADA, REQUIERE_INFORMACION_ADICIONAL, CANCELADA);
		}
	},
	PENDIENTE {
		@Override
		public Set<EstadoRadicacion> estadosPermitidos() {
			return Set.of(EN_REVISION, CANCELADA);
		}
	};

	// Por defecto, un estado (como los finales) no puede transicionar a ningun otro
	public Set<EstadoRadicacion> estadosPermitidos() {
		return Set.of();
	}

	public boolean puedeTransicionarA(EstadoRadicacion nuevoEstado) {
		return estadosPermitidos().contains(nuevoEstado);
	}

	public boolean esFinal() {
		return this == RADICADA || this == RECHAZADA || this == CANCELADA;
	}
}