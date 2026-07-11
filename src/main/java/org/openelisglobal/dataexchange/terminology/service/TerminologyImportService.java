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
 * Service d'import de codes terminologiques (LOINC / SNOMED CT) depuis un CSV
 * (séparateur ';', UTF-8). Le matching se fait toujours par clé naturelle, jamais
 * par ID, pour rester robuste entre environnements. Voir
 * src/main/resources/terminology/README.md pour le format attendu.
 */
public interface TerminologyImportService {

    /**
     * Dry-run : analyse le CSV et renvoie ce qui serait fait, sans rien écrire en
     * base (les lignes applicables sortent en WOULD_UPDATE).
     *
     * @param overwrite si false, une ligne dont le code diffère d'une valeur déjà
     *                  présente en base sort en CONFLICT (non appliquée) ; si true,
     *                  elle écraserait l'existant (WOULD_UPDATE).
     */
    TerminologyImportReport preview(TerminologyTarget target, String csvContent, boolean overwrite);

    /**
     * Applique le CSV : met à jour les entités correspondantes (les lignes
     * appliquées sortent en UPDATED). Idempotent et rejouable.
     *
     * @param overwrite si false, les conflits (code différent d'une valeur existante)
     *                  ne sont pas appliqués et sortent en CONFLICT ; si true, ils
     *                  écrasent l'existant.
     */
    TerminologyImportReport apply(TerminologyTarget target, String csvContent, boolean overwrite);
}
