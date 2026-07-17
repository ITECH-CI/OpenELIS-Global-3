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
package org.openelisglobal.dataexchange.sync.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.dataexchange.sync.valueholder.AnalysisSyncStatus;

/**
 * Accès à la file CDC {@code analysis_sync_status} (remontée consolidée,
 * incrément 4b). Les transitions d'état se font par UPDATE de masse ciblé sur
 * l'{@code id} des lignes du lot (miroir du batchUpdate uploader), pas par
 * load-modify-save.
 */
public interface AnalysisSyncStatusDAO extends BaseDAO<AnalysisSyncStatus, String> {

    /**
     * Sélectionne un lot d'analyses à remonter : {@code upload_flag IN (1,2)},
     * TO_INSERT puis TO_UPDATE, plus anciennes d'abord.
     *
     * @param batchSize taille max du lot
     */
    List<AnalysisSyncStatus> getBatchToSync(int batchSize);

    /** Passe les lignes (par id) à IN_PROGRESS (4). Renvoie le nombre modifié. */
    int markInProgress(List<String> ids);

    /** Passe les lignes (par id) à UP_TO_DATE (3) après remontée réussie. */
    int markUpToDate(List<String> ids);

    /**
     * Repasse les lignes (par id) à TO_UPDATE (2) pour re-tentative après échec.
     */
    int markToUpdate(List<String> ids);

    /**
     * Reset des lignes coincées en IN_PROGRESS (4) au-delà du délai (crash/restart)
     * : repassées à TO_UPDATE (2).
     *
     * @param timeoutMinutes ancienneté au-delà de laquelle une ligne IN_PROGRESS
     *                       est considérée orpheline
     * @return nombre de lignes réinitialisées
     */
    int resetStaleInProgress(int timeoutMinutes);
}
