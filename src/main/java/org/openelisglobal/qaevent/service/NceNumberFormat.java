package org.openelisglobal.qaevent.service;

/**
 * Forme du numéro d'événement non conforme : {@code ENC-<site>-<aa>-<nnnn>},
 * par exemple {@code ENC-14001-26-0014}. Sans préfixe de site, le champ et son
 * séparateur sont omis : {@code ENC-26-0014}.
 */
public final class NceNumberFormat {

    static final String LITERAL = "ENC";

    /** Longueur de la colonne nc_event.nce_number. */
    public static final int MAX_LENGTH = 20;

    /** Au-delà, le compteur passe à cinq chiffres ou plus, sans jamais boucler. */
    public static final long FOUR_DIGIT_MAX = 9999;

    private NceNumberFormat() {
    }

    /** Préfixe sans espaces ; une valeur absente vaut vide. */
    public static String normalizeSite(String sitePrefix) {
        return sitePrefix == null ? "" : sitePrefix.trim();
    }

    /**
     * Clé du compteur : un compteur par site et par année. Le séparateur évite
     * toute confusion avec le préfixe des numéros de laboratoire.
     */
    public static String counterKey(String sitePrefix, String twoDigitYear) {
        return String.join("-", head(sitePrefix, twoDigitYear));
    }

    public static String format(String sitePrefix, String twoDigitYear, long sequence) {
        String number = sequence <= FOUR_DIGIT_MAX ? String.format("%04d", sequence) : String.valueOf(sequence);
        return counterKey(sitePrefix, twoDigitYear) + "-" + number;
    }

    private static String[] head(String sitePrefix, String twoDigitYear) {
        String site = normalizeSite(sitePrefix);
        return site.isEmpty() ? new String[] { LITERAL, twoDigitYear } : new String[] { LITERAL, site, twoDigitYear };
    }
}
