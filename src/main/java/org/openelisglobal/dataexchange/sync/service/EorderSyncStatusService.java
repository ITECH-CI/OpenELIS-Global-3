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
import org.openelisglobal.dataexchange.sync.valueholder.EorderSyncStatus;

/**
 * Gestion de la file {@code eorder_sync_status} du push de statuts (incrément
 * 4d). Transitions par méthodes PUBLIQUES {@code @Transactional}.
 */
public interface EorderSyncStatusService extends BaseObjectService<EorderSyncStatus, String> {

    /** Lot à pousser (sync_flag IN (2,6)), borné par batchSize. */
    List<EorderSyncStatus> getBatchToPush(int batchSize);

    /** Marque une ligne IN_PROGRESS (4). */
    void markInProgress(String id);

    /**
     * Marque une ligne SYNCED (3) après push confirmé, et mémorise le
     * statut/résultat envoyés (anti-régression / anti-boucle) + labno + eventUuid.
     */
    void markSynced(String id, String sentLabStatus, String sentResult, String labno, String eventUuid);

    /** Repasse une ligne à SEND_FAILED (6) pour retry au prochain cycle. */
    void markSendFailed(String id);

    /** Repasse une ligne à TO_UPDATE (2) (ex. exception non gérée). */
    void markToUpdate(String id);

    /** Réarme les lignes IN_PROGRESS orphelines (crash/restart) à TO_UPDATE (2). */
    int resetStaleInProgress(int timeoutMinutes);
}
