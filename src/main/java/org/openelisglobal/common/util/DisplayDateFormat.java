package org.openelisglobal.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Date d'affichage jj/mm/aaaa ou mm/jj/aaaa. La date est lue d'abord dans
 * l'ordre du réglage « default date locale » : une date ambiguë (jour inférieur
 * à 13) garde ainsi son sens, et la réécriture d'une date déjà au bon format la
 * laisse inchangée.
 */
public final class DisplayDateFormat {

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private DisplayDateFormat() {
    }

    /** Vrai si le réglage « default date locale » place le jour en premier. */
    public static boolean isDayFirst(String defaultDateLocale) {
        return "fr-FR".equals(defaultDateLocale);
    }

    /**
     * La date réécrite au format du réglage, ou « Invalid date format: … » si elle
     * ne se lit dans aucun des deux ordres (comportement de
     * DateUtil.formatStringDate).
     */
    public static String reformat(String value, boolean dayFirst) {
        if (value == null) {
            return "";
        }
        DateTimeFormatter preferred = dayFirst ? DAY_MONTH : MONTH_DAY;
        DateTimeFormatter other = dayFirst ? MONTH_DAY : DAY_MONTH;
        LocalDate date;
        try {
            date = LocalDate.parse(value, preferred);
        } catch (DateTimeParseException e) {
            try {
                date = LocalDate.parse(value, other);
            } catch (DateTimeParseException ex) {
                return "Invalid date format: " + value;
            }
        }
        return date.format(preferred);
    }
}
