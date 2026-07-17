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

import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.sync.dao.AnalysisSyncStatusDAO;
import org.openelisglobal.dataexchange.sync.valueholder.AnalysisSyncStatus;
import org.openelisglobal.dataexchange.sync.valueholder.AnalysisSyncStatus.AnalysisSyncFlag;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalysisSyncStatusDAOImpl extends BaseDAOImpl<AnalysisSyncStatus, String>
        implements AnalysisSyncStatusDAO {

    public AnalysisSyncStatusDAOImpl() {
        super(AnalysisSyncStatus.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisSyncStatus> getBatchToSync(int batchSize) {
        try {
            String hql = "from AnalysisSyncStatus a where a.uploadFlag in (:toInsert, :toUpdate)"
                    + " order by a.uploadFlag asc, a.lastUpdated asc";
            Query<AnalysisSyncStatus> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalysisSyncStatus.class);
            query.setParameter("toInsert", AnalysisSyncFlag.TO_INSERT);
            query.setParameter("toUpdate", AnalysisSyncFlag.TO_UPDATE);
            if (batchSize > 0) {
                query.setMaxResults(batchSize);
            }
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AnalysisSyncStatus getBatchToSync()", e);
        }
    }

    @Override
    public int markInProgress(List<String> ids) {
        return bulkSetFlag(ids, AnalysisSyncFlag.IN_PROGRESS, true);
    }

    @Override
    public int markUpToDate(List<String> ids) {
        // Comme l'uploader, la transition vers UP_TO_DATE ne touche PAS last_updated
        // (sinon un changement métier concurrent en 3->2 pourrait être masqué par un
        // last_updated plus récent côté succès).
        return bulkSetFlag(ids, AnalysisSyncFlag.UP_TO_DATE, false);
    }

    @Override
    public int markToUpdate(List<String> ids) {
        return bulkSetFlag(ids, AnalysisSyncFlag.TO_UPDATE, true);
    }

    private int bulkSetFlag(List<String> ids, short flag, boolean touchLastUpdated) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        try {
            String hql = "update AnalysisSyncStatus a set a.uploadFlag = :flag"
                    + (touchLastUpdated ? ", a.lastUpdated = current_timestamp()" : "") + " where a.id in (:ids)";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(hql);
            query.setParameter("flag", flag);
            query.setParameterList("ids", ids);
            return query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AnalysisSyncStatus bulkSetFlag()", e);
        }
    }

    @Override
    public int resetStaleInProgress(int timeoutMinutes) {
        try {
            // Interval Postgres : les lignes IN_PROGRESS plus vieilles que le délai sont
            // considérées orphelines (crash/restart) et repassées à TO_UPDATE.
            String sql = "UPDATE clinlims.analysis_sync_status SET upload_flag = :toUpdate, last_updated = now()"
                    + " WHERE upload_flag = :inProgress AND last_updated < now() - (:minutes || ' minutes')::interval";
            Query<?> query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("toUpdate", (int) AnalysisSyncFlag.TO_UPDATE);
            query.setParameter("inProgress", (int) AnalysisSyncFlag.IN_PROGRESS);
            query.setParameter("minutes", String.valueOf(timeoutMinutes));
            return query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AnalysisSyncStatus resetStaleInProgress()", e);
        }
    }

    /** Évite les avertissements sur liste immuable partagée. */
    @SuppressWarnings("unused")
    private static List<String> emptyIfNull(List<String> ids) {
        return ids == null ? Collections.emptyList() : ids;
    }
}
