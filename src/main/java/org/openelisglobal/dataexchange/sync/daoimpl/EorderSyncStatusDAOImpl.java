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
package org.openelisglobal.dataexchange.sync.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.sync.dao.EorderSyncStatusDAO;
import org.openelisglobal.dataexchange.sync.valueholder.EorderSyncStatus;
import org.openelisglobal.dataexchange.sync.valueholder.EorderSyncStatus.EorderSyncFlag;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EorderSyncStatusDAOImpl extends BaseDAOImpl<EorderSyncStatus, String> implements EorderSyncStatusDAO {

    public EorderSyncStatusDAOImpl() {
        super(EorderSyncStatus.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EorderSyncStatus> getBatchToPush(int batchSize) {
        try {
            String hql = "from EorderSyncStatus e where e.syncFlag in (:toUpdate, :sendFailed)"
                    + " order by e.updatedAt asc";
            Query<EorderSyncStatus> query = entityManager.unwrap(Session.class).createQuery(hql,
                    EorderSyncStatus.class);
            query.setParameter("toUpdate", EorderSyncFlag.TO_UPDATE);
            query.setParameter("sendFailed", EorderSyncFlag.SEND_FAILED);
            if (batchSize > 0) {
                query.setMaxResults(batchSize);
            }
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EorderSyncStatus getBatchToPush()", e);
        }
    }

    @Override
    public int resetStaleInProgress(int timeoutMinutes) {
        try {
            // Les lignes IN_PROGRESS plus vieilles que le délai sont considérées
            // orphelines (crash/restart) et repassées à TO_UPDATE pour re-push.
            String sql = "UPDATE clinlims.eorder_sync_status SET sync_flag = :toUpdate, updated_at = now()"
                    + " WHERE sync_flag = :inProgress AND updated_at < now() - (:minutes || ' minutes')::interval";
            Query<?> query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("toUpdate", (int) EorderSyncFlag.TO_UPDATE);
            query.setParameter("inProgress", (int) EorderSyncFlag.IN_PROGRESS);
            query.setParameter("minutes", String.valueOf(timeoutMinutes));
            return query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in EorderSyncStatus resetStaleInProgress()", e);
        }
    }
}
