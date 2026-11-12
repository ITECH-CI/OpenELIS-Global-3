package org.openelisglobal.patient.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Numéro CMU de l'assuré (le même numéro que le matricule CNAM et le numéro de
 * sécurité sociale).
 *
 * <p>
 * Normalisation : on retire les espaces, les tirets et les points. Format :
 * exactement 13 chiffres par défaut (réglage
 * {@code org.openelisglobal.cmu.format}). Seule une valeur normalisée et
 * conforme est émise ou retenue ; une valeur non conforme n'est jamais
 * corrigée.
 */
public final class CmuNumber {

    public static final String DEFAULT_FORMAT = "^[0-9]{13}$";

    private static final Pattern SEPARATORS = Pattern.compile("[\\s.\\-]");
    private static final Map<String, Pattern> FORMATS = new ConcurrentHashMap<>();

    private CmuNumber() {
    }

    /** Retire espaces, tirets et points ; {@code null} reste {@code null}. */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return SEPARATORS.matcher(value).replaceAll("");
    }

    /**
     * Vrai si la valeur, une fois normalisée, respecte le format (vide ou invalide
     * : format par défaut).
     */
    public static boolean conforms(String value, String format) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isEmpty()) {
            return false;
        }
        return pattern(format).matcher(normalized).matches();
    }

    /** La valeur normalisée si elle est conforme, sinon {@code null}. */
    public static String conformingOrNull(String value, String format) {
        return conforms(value, format) ? normalize(value) : null;
    }

    /** Vrai si la valeur est vide une fois normalisée. */
    public static boolean isBlank(String value) {
        String normalized = normalize(value);
        return normalized == null || normalized.isEmpty();
    }

    private static Pattern pattern(String format) {
        String effective = format == null || format.trim().isEmpty() ? DEFAULT_FORMAT : format.trim();
        return FORMATS.computeIfAbsent(effective, f -> {
            try {
                return Pattern.compile(f);
            } catch (PatternSyntaxException e) {
                return Pattern.compile(DEFAULT_FORMAT);
            }
        });
    }
}
