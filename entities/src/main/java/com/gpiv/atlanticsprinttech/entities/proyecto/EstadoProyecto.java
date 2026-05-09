package com.gpiv.atlanticsprinttech.entities.proyecto;

import java.util.Set;

public enum EstadoProyecto {
	COMPLETADO,
	CANCELADO,
	DETENIDO {
		@Override
		public Set<EstadoProyecto> estadosPermitidos() {
			return Set.of(EN_EJECUCION, CANCELADO);
		}
	},
	EN_EJECUCION {
		@Override
		public Set<EstadoProyecto> estadosPermitidos() {
			return Set.of(DETENIDO, COMPLETADO, CANCELADO);
		}
	},
	INICIADO {
		@Override
		public Set<EstadoProyecto> estadosPermitidos() {
			return Set.of(EN_EJECUCION, CANCELADO);
		}
	};

	public Set<EstadoProyecto> estadosPermitidos() {
		return Set.of();
	}

	public boolean puedeTransicionarA(EstadoProyecto nuevoEstado) {
		return estadosPermitidos().contains(nuevoEstado);
	}

	public boolean esFinal() {
		return this == COMPLETADO || this == CANCELADO;
	}
}