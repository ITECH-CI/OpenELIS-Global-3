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

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.dataexchange.sync.dao.EorderSyncStatusDAO;
import org.openelisglobal.dataexchange.sync.valueholder.EorderSyncStatus;
import org.openelisglobal.dataexchange.sync.valueholder.EorderSyncStatus.EorderSyncFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EorderSyncStatusServiceImpl extends BaseObjectServiceImpl<EorderSyncStatus, String>
        implements EorderSyncStatusService {

    @Autowired
    protected EorderSyncStatusDAO baseObjectDAO;

    EorderSyncStatusServiceImpl() {
        super(EorderSyncStatus.class);
    }

    @Override
    protected EorderSyncStatusDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EorderSyncStatus> getBatchToPush(int batchSize) {
        return baseObjectDAO.getBatchToPush(batchSize);
    }

    @Override
    @Transactional
    public void markInProgress(String id) {
        EorderSyncStatus row = baseObjectDAO.get(id).orElse(null);
        if (row == null) {
            return;
        }
        row.setSyncFlag(EorderSyncFlag.IN_PROGRESS);
        row.setUpdatedAt(now());
        baseObjectDAO.update(row);
    }

    @Override
    @Transactional
    public void markSynced(String id, String sentLabStatus, String sentResult, String labno, String eventUuid) {
        EorderSyncStatus row = baseObjectDAO.get(id).orElse(null);
        if (row == null) {
            return;
        }
        // Garde optimiste : ne passe à SYNCED que si la ligne est TOUJOURS en
        // IN_PROGRESS (4). Si un trigger l'a repassée à TO_UPDATE (2) pendant le push
        // (changement concurrent), on mémorise le last_sent envoyé MAIS on laisse le
        // flag à 2 pour un nouveau push — sinon le changement concurrent serait perdu.
        boolean stillInProgress = row.getSyncFlag() != null && row.getSyncFlag() == EorderSyncFlag.IN_PROGRESS;
        if (stillInProgress) {
            row.setSyncFlag(EorderSyncFlag.SYNCED);
        }
        row.setLabStatus(sentLabStatus);
        row.setLastSentLabStatus(sentLabStatus);
        row.setLastSentResult(sentResult);
        if (labno != null) {
            row.setLabno(labno);
        }
        row.setLastEventUuid(eventUuid);
        row.setLastSyncedAt(now());
        row.setUpdatedAt(now());
        baseObjectDAO.update(row);
    }

    @Override
    @Transactional
    public void markSendFailed(String id) {
        setFlag(id, EorderSyncFlag.SEND_FAILED);
    }

    @Override
    @Transactional
    public void markToUpdate(String id) {
        setFlag(id, EorderSyncFlag.TO_UPDATE);
    }

    @Override
    @Transactional
    public int resetStaleInProgress(int timeoutMinutes) {
        return baseObjectDAO.resetStaleInProgress(timeoutMinutes);
    }

    private void setFlag(String id, short flag) {
        EorderSyncStatus row = baseObjectDAO.get(id).orElse(null);
        if (row == null) {
            return;
        }
        row.setSyncFlag(flag);
        row.setUpdatedAt(now());
        baseObjectDAO.update(row);
    }

    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
