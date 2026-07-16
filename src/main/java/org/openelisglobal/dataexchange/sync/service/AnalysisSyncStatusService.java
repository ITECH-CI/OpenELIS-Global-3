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
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dataexchange.sync.valueholder.AnalysisSyncStatus;

/**
 * Gestion de la file CDC {@code analysis_sync_status} pour la remontée
 * consolidée (incrément 4b). Toutes les transitions d'état sont des méthodes
 * PUBLIQUES {@code @Transactional} (le proxy Spring s'applique — contrairement
 * à l'uploader d'origine où ces méthodes étaient privées, donc non
 * transactionnelles).
 */
public interface AnalysisSyncStatusService extends BaseObjectService<AnalysisSyncStatus, String> {

    /** Un lot d'analyses à remonter (upload_flag IN (1,2)), borné par batchSize. */
    List<AnalysisSyncStatus> getBatchToSync(int batchSize);

    /** Marque le lot IN_PROGRESS (4). */
    int markInProgress(List<String> ids);

    /** Marque le lot UP_TO_DATE (3) après remontée réussie. */
    int markUpToDate(List<String> ids);

    /** Repasse le lot à TO_UPDATE (2) pour re-tentative après échec. */
    int markToUpdate(List<String> ids);

    /**
     * Réinitialise les lignes IN_PROGRESS orphelines (crash/restart) à TO_UPDATE
     * (2).
     */
    int resetStaleInProgress(int timeoutMinutes);
}
