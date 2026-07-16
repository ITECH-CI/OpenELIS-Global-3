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
import org.openelisglobal.dataexchange.sync.valueholder.EorderSyncStatus;

/**
 * Accès à la file {@code eorder_sync_status} du push de statuts (incrément 4d).
 * La file à pousser = {@code sync_flag IN (2 TO_UPDATE, 6 SEND_FAILED)}, FIFO
 * par {@code updated_at}.
 */
public interface EorderSyncStatusDAO extends BaseDAO<EorderSyncStatus, String> {

    /**
     * Lot à pousser (sync_flag IN (2,6)), plus anciens d'abord, borné par
     * batchSize.
     */
    List<EorderSyncStatus> getBatchToPush(int batchSize);

    /**
     * Réarme les lignes coincées en IN_PROGRESS (4) au-delà du délai (crash/restart
     * entre markInProgress et markSynced) : repassées à TO_UPDATE (2).
     *
     * @return nombre de lignes réarmées
     */
    int resetStaleInProgress(int timeoutMinutes);
}
