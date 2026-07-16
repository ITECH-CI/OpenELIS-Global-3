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
 * <p>Copyright (C) ITECH-CI. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.sync.service;

import java.util.List;
import org.openelisglobal.dataexchange.sync.dto.DataSyncPayload;

/**
 * Construit le {@link DataSyncPayload} d'un lot d'analyses à remonter, à partir
 * des SERVICES MÉTIER OE (pas de SQL en dur) : décision d'incrément 4b. Le
 * builder ne touche pas au statut ni au transport — il assemble seulement les
 * DTO.
 */
public interface DataSyncPayloadBuilder {

    /**
     * Assemble le payload pour les analyses dont les identifiants sont fournis (les
     * {@code analysis_id} du lot sélectionné dans {@code analysis_sync_status}).
     *
     * @param analysisIds identifiants d'analyses à remonter
     * @return payload prêt à sérialiser (jamais null ; listes vides si rien)
     */
    DataSyncPayload buildForAnalysisIds(List<String> analysisIds);
}
