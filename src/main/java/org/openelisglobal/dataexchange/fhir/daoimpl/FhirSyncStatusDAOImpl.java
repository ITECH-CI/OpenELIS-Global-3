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
package org.openelisglobal.dataexchange.fhir.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.dao.FhirSyncStatusDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirSyncStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FhirSyncStatusDAOImpl extends BaseDAOImpl<FhirSyncStatus, String> implements FhirSyncStatusDAO {

    public FhirSyncStatusDAOImpl() {
        super(FhirSyncStatus.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FhirSyncStatus> getByStatus(String status, int maxResults) {
        try {
            String hql = "from FhirSyncStatus f where f.status = :status"
                    + " order by f.lastAttemptAt asc nulls first";
            Query<FhirSyncStatus> query = entityManager.unwrap(Session.class).createQuery(hql, FhirSyncStatus.class);
            query.setParameter("status", status);
            if (maxResults > 0) {
                query.setMaxResults(maxResults);
            }
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in FhirSyncStatus getByStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FhirSyncStatus> getRetryable(int maxAttempts, int maxResults) {
        try {
            String hql = "from FhirSyncStatus f where f.status = 'FAILED' and f.attemptCount < :maxAttempts"
                    + " order by f.lastAttemptAt asc nulls first";
            Query<FhirSyncStatus> query = entityManager.unwrap(Session.class).createQuery(hql, FhirSyncStatus.class);
            query.setParameter("maxAttempts", maxAttempts);
            if (maxResults > 0) {
                query.setMaxResults(maxResults);
            }
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in FhirSyncStatus getRetryable()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FhirSyncStatus findLatestByTarget(String targetType, String targetId, String triggerType) {
        try {
            String hql = "from FhirSyncStatus f where f.targetType = :targetType and f.targetId = :targetId"
                    + " and f.triggerType = :triggerType order by f.lastAttemptAt desc nulls last";
            Query<FhirSyncStatus> query = entityManager.unwrap(Session.class).createQuery(hql, FhirSyncStatus.class);
            query.setParameter("targetType", targetType);
            query.setParameter("targetId", targetId);
            query.setParameter("triggerType", triggerType);
            query.setMaxResults(1);
            List<FhirSyncStatus> list = query.list();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in FhirSyncStatus findLatestByTarget()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        try {
            String hql = "select count(f) from FhirSyncStatus f where f.status = :status";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("status", status);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in FhirSyncStatus countByStatus()", e);
        }
    }
}
