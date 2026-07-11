/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.dataexchange.terminology.service;

/**
 * Cibles d'import terminologique. Chaque cible correspond à une entité codable
 * (colonnes LOINC / SNOMED) et à un jeu de clés naturelles dans le CSV.
 */
public enum TerminologyTarget {

    /** Tests : clé test_name (+ sample_type indicatif). Colonnes loinc + snomed. */
    TEST,

    /** Entrées de dictionnaire : clé category + dict_entry. Colonnes loinc + snomed. */
    DICTIONARY,

    /** Types d'observation clinique : clé type_name. Colonnes loinc + snomed. */
    OBSERVATION_HISTORY_TYPE;

    /**
     * Résolution tolérante depuis une chaîne (insensible à la casse, trim). Renvoie
     * null si aucune cible ne correspond plutôt que de lever une exception.
     */
    public static TerminologyTarget fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        for (TerminologyTarget target : values()) {
            if (target.name().equals(normalized)) {
                return target;
            }
        }
        return null;
    }
}
