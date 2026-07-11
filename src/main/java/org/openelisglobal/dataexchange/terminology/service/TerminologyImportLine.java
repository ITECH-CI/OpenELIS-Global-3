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
 * Résultat d'import pour une ligne du CSV : clé lisible, action retenue, codes et
 * message éventuel. Sérialisé tel quel en JSON dans le rapport.
 */
public class TerminologyImportLine {

    /** Actions possibles par ligne. */
    public enum Action {
        /** Dry-run : la ligne serait mise à jour si on appliquait. */
        WOULD_UPDATE,
        /** Ligne effectivement appliquée en base. */
        UPDATED,
        /** Aucune entité ne correspond à la clé naturelle. */
        NOT_FOUND,
        /** Ligne ignorée car status != validated. */
        SKIPPED_PROPOSED,
        /** Ligne ignorée car aucun code (loinc/snomed) fourni. */
        SKIPPED_NO_CODE,
        /** Plusieurs entités correspondent à la clé : refus de trancher. */
        AMBIGUOUS,
        /** Erreur inattendue lors du traitement de la ligne (voir message). */
        ERROR
    }

    private String key;
    private Action action;
    private String loinc;
    private String snomed;
    private String message;

    public TerminologyImportLine() {
    }

    public TerminologyImportLine(String key, Action action, String loinc, String snomed, String message) {
        this.key = key;
        this.action = action;
        this.loinc = loinc;
        this.snomed = snomed;
        this.message = message;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getLoinc() {
        return loinc;
    }

    public void setLoinc(String loinc) {
        this.loinc = loinc;
    }

    public String getSnomed() {
        return snomed;
    }

    public void setSnomed(String snomed) {
        this.snomed = snomed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
