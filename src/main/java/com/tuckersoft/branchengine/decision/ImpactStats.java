package com.tuckersoft.branchengine.decision;

import java.util.Map;

/** Delta de lucidity/controlLevel segun impactLevel. Limites 0-100 se aplican aparte. */
public final class ImpactStats {

    public record Delta(int lucidity, int controlLevel) {
    }

    private static final Map<String, Delta> TABLE = Map.of(
            "LEVE", new Delta(-5, 5),
            "MODERADO", new Delta(-15, 10),
            "GRAVE", new Delta(-30, 20),
            "CRITICO", new Delta(-40, 45)
    );

    private ImpactStats() {
    }

    public static Delta forImpact(String impactLevel) {
        Delta delta = TABLE.get(impactLevel);
        if (delta == null) {
            throw new IllegalStateException("impactLevel desconocido: " + impactLevel);
        }
        return delta;
    }
}
