package com.tuckersoft.branchengine.decision;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Motor de reglas propias (sin IA). Orden de reglas, verificado contra
 * autotests/Checkpoint4Decisiones.java:
 * 1 ENTRADA_CORRUPTA  2 RUPTURA_CUARTA_PARED  3 SOSPECHA  4 REBELDIA  5 OBEDIENCIA.
 * La primera regla que se cumple gana; el orden importa (ej: "destruye la camara"
 * es RUPTURA_CUARTA_PARED, no REBELDIA, porque la regla 2 se evalua antes).
 */
@Component
public class DecisionClassifier {

    private static final Pattern LETRA_A_Z = Pattern.compile("[a-z]");

    public String normalize(String rawInput) {
        String sinTildes = Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase();
    }

    public String classify(String rawInput) {
        String texto = normalize(rawInput);

        if (!LETRA_A_Z.matcher(texto).find()) {
            return "ENTRADA_CORRUPTA";
        }
        if (containsAny(texto, "netflix", "camara", "espectador", "videojuego")) {
            return "RUPTURA_CUARTA_PARED";
        }
        if (containsAny(texto, "vigilan", "simbolo", "conspiracion")) {
            return "SOSPECHA";
        }
        if (containsAny(texto, "rechaza", "destruye", "desobedece", "renuncia")) {
            return "REBELDIA";
        }
        return "OBEDIENCIA";
    }

    private boolean containsAny(String texto, String... palabras) {
        for (String palabra : palabras) {
            if (texto.contains(palabra)) {
                return true;
            }
        }
        return false;
    }
}
