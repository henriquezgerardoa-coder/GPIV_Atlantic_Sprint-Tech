package com.gpiv.atlanticsprinttech.entities.dominio;

import java.util.Set;

public enum EtapaCicloLote {

    DISPONIBLE(null, Set.of()) {
        @Override public Set<EtapaCicloLote> transicionesPermitidas() {
            return Set.of(PROYECTO_EN_EVALUACION);
        }
    },
    PROYECTO_EN_EVALUACION(90, Set.of()) {
        @Override public Set<EtapaCicloLote> transicionesPermitidas() {
            return Set.of(ADJUDICADO_PRECARIO, REVERTIDO, DISPONIBLE);
        }
    },
    ADJUDICADO_PRECARIO(180, Set.of()) {
        @Override public Set<EtapaCicloLote> transicionesPermitidas() {
            return Set.of(EN_CONSTRUCCION, OPERATIVO, REVERTIDO);
        }
    },
    EN_CONSTRUCCION(540, Set.of()) {
        @Override public Set<EtapaCicloLote> transicionesPermitidas() {
            return Set.of(OPERATIVO, REVERTIDO);
        }
    },
    OPERATIVO(null, Set.of()) {
        @Override public Set<EtapaCicloLote> transicionesPermitidas() {
            return Set.of(ESCRITURADO, REVERTIDO);
        }
    },
    ESCRITURADO(null, Set.of()) {
        @Override public Set<EtapaCicloLote> transicionesPermitidas() {
            return Set.of();
        }
    },
    REVERTIDO(null, Set.of()) {
        @Override public Set<EtapaCicloLote> transicionesPermitidas() {
            return Set.of(DISPONIBLE);
        }
    };

    private final Integer plazoDiasDefault;
    @SuppressWarnings("unused")
    private final Set<EtapaCicloLote> _placeholder;

    EtapaCicloLote(Integer plazoDiasDefault, Set<EtapaCicloLote> placeholder) {
        this.plazoDiasDefault = plazoDiasDefault;
        this._placeholder = placeholder;
    }

    public abstract Set<EtapaCicloLote> transicionesPermitidas();

    public boolean puedeTransicionarA(EtapaCicloLote nueva) {
        return transicionesPermitidas().contains(nueva);
    }

    public Integer plazoDiasDefault() {
        return plazoDiasDefault;
    }

    public boolean esTerminal() {
        return this == ESCRITURADO;
    }

    public boolean tienePlazo() {
        return plazoDiasDefault != null;
    }

    public String colorHexMapa() {
        return switch (this) {
            case DISPONIBLE             -> "#dee2e6";
            case PROYECTO_EN_EVALUACION -> "#fff3cd";
            case ADJUDICADO_PRECARIO    -> "#ffc107";
            case EN_CONSTRUCCION        -> "#fd7e14";
            case OPERATIVO              -> "#198754";
            case ESCRITURADO            -> "#0d6efd";
            case REVERTIDO              -> "#dc3545";
        };
    }

    public String etiquetaLegible() {
        return switch (this) {
            case DISPONIBLE             -> "Disponible";
            case PROYECTO_EN_EVALUACION -> "Proyecto en evaluación";
            case ADJUDICADO_PRECARIO    -> "Adjudicado precario";
            case EN_CONSTRUCCION        -> "En construcción";
            case OPERATIVO              -> "Operativo";
            case ESCRITURADO            -> "Escriturado";
            case REVERTIDO              -> "Revertido";
        };
    }

    public String estadoAsignacionLegacy() {
        return switch (this) {
            case PROYECTO_EN_EVALUACION                         -> "PREADJUDICADO";
            case ADJUDICADO_PRECARIO, EN_CONSTRUCCION,
                 OPERATIVO, ESCRITURADO                         -> "ADJUDICADO";
            case REVERTIDO                                      -> "DESADJUDICADO";
            case DISPONIBLE                                     -> null;
        };
    }
}
