package com.tuckersoft.branchengine.decision;

import java.util.Map;

/** handlerUnit / outcomeCode: se derivan del branchType, nunca vienen en el request. */
public final class BranchMetadata {

    public record Meta(String handlerUnit, String outcomeCode) {
    }

    private static final Map<String, Meta> TABLE = Map.of(
            "OBEDIENCIA", new Meta("Mesa de Guion", "ADVANCE_MAIN_PATH"),
            "REBELDIA", new Meta("Control de Continuidad", "FORK_TIMELINE"),
            "SOSPECHA", new Meta("Oficina de Seguridad", "INJECT_WHITE_BEAR_SYMBOL"),
            "RUPTURA_CUARTA_PARED", new Meta("Departamento Netflix", "BREAK_FOURTH_WALL"),
            "ENTRADA_CORRUPTA", new Meta("Archivo de Errores", "DISCARD_INPUT")
    );

    private BranchMetadata() {
    }

    public static Meta forBranchType(String branchType) {
        Meta meta = TABLE.get(branchType);
        if (meta == null) {
            throw new IllegalStateException("branchType desconocido: " + branchType);
        }
        return meta;
    }
}
