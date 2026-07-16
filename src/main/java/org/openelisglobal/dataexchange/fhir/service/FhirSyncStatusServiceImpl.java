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
package org.openelisglobal.dataexchange.fhir.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.dataexchange.fhir.FhirSyncConstants;
import org.openelisglobal.dataexchange.fhir.dao.FhirSyncStatusDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirSyncStatus;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirSyncStatus.SyncStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FhirSyncStatusServiceImpl extends BaseObjectServiceImpl<FhirSyncStatus, String>
        implements FhirSyncStatusService {

    private static final int MAX_ERROR_LENGTH = 2000;

    @Autowired
    protected FhirSyncStatusDAO baseObjectDAO;

    FhirSyncStatusServiceImpl() {
        super(FhirSyncStatus.class);
    }

    @Override
    protected FhirSyncStatusDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    // REQUIRES_NEW : le traçage vit dans sa PROPRE transaction, indépendante de la
    // transformation. Ainsi un rollback de la transfo n'efface pas la trace, et une
    // erreur de traçage ne casse pas la transfo (le traçage ne doit JAMAIS être
    // bloquant — d'où aussi les try/catch défensifs).
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String recordPending(String triggerType, String targetType, String targetId) {
        try {
            FhirSyncStatus status = null;
            if (targetType != null && targetId != null) {
                status = baseObjectDAO.findLatestByTarget(targetType, targetId, triggerType);
            }
            if (status == null) {
                status = new FhirSyncStatus();
                status.setTriggerType(triggerType);
                status.setTargetType(targetType);
                status.setTargetId(targetId);
                status.setAttemptCount(0);
            }
            status.setStatus(SyncStatus.PENDING.name());
            status.setLastAttemptAt(Timestamp.from(Instant.now()));
            status.setSysUserId("1");
            FhirSyncStatus saved = save(status);
            return saved.getId();
        } catch (Exception e) {
            // Le traçage ne doit jamais faire échouer la transformation.
            LogEvent.logError("FhirSyncStatusServiceImpl", "recordPending", e.toString());
            return null;
        }
    }

    @Override
    public Map<String, String> recordPendingForSamples(String triggerType, Collection<String> sampleIds) {
        Map<String, String> result = new HashMap<>();
        if (sampleIds == null) {
            return result;
        }
        for (String sampleId : sampleIds) {
            if (sampleId == null) {
                continue;
            }
            String syncId = recordPending(triggerType, FhirSyncConstants.TARGET_SAMPLE, sampleId);
            if (syncId != null) {
                result.put(sampleId, syncId);
            }
        }
        return result;
    }

    @Override
    public void markSuccessForAll(Collection<String> syncStatusIds) {
        if (syncStatusIds == null) {
            return;
        }
        for (String id : syncStatusIds) {
            markSuccess(id);
        }
    }

    @Override
    public void markFailedForAll(Collection<String> syncStatusIds, String errorMessage) {
        if (syncStatusIds == null) {
            return;
        }
        for (String id : syncStatusIds) {
            markFailed(id, errorMessage);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String syncStatusId) {
        if (syncStatusId == null) {
            return;
        }
        try {
            FhirSyncStatus status = get(syncStatusId);
            if (status == null) {
                return;
            }
            status.setStatus(SyncStatus.SUCCESS.name());
            status.setErrorMessage(null);
            status.setLastAttemptAt(Timestamp.from(Instant.now()));
            status.setSysUserId("1");
            update(status);
        } catch (Exception e) {
            LogEvent.logError("FhirSyncStatusServiceImpl", "markSuccess", e.toString());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccessWithIssues(String syncStatusId, java.util.List<String> issues) {
        if (syncStatusId == null) {
            return;
        }
        if (issues == null || issues.isEmpty()) {
            markSuccess(syncStatusId);
            return;
        }
        try {
            FhirSyncStatus status = get(syncStatusId);
            if (status == null) {
                return;
            }
            status.setStatus(SyncStatus.SUCCESS_INCOMPLETE.name());
            status.setErrorMessage(truncate(String.join(" | ", issues)));
            status.setLastAttemptAt(Timestamp.from(Instant.now()));
            status.setSysUserId("1");
            update(status);
        } catch (Exception e) {
            LogEvent.logError("FhirSyncStatusServiceImpl", "markSuccessWithIssues", e.toString());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String syncStatusId, String errorMessage) {
        if (syncStatusId == null) {
            return;
        }
        try {
            FhirSyncStatus status = get(syncStatusId);
            if (status == null) {
                return;
            }
            status.setStatus(SyncStatus.FAILED.name());
            int attempts = status.getAttemptCount() == null ? 0 : status.getAttemptCount();
            status.setAttemptCount(attempts + 1);
            status.setErrorMessage(truncate(errorMessage));
            status.setLastAttemptAt(Timestamp.from(Instant.now()));
            status.setSysUserId("1");
            update(status);
        } catch (Exception e) {
            LogEvent.logError("FhirSyncStatusServiceImpl", "markFailed", e.toString());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FhirSyncStatus> getRetryable(int maxAttempts, int maxResults) {
        return baseObjectDAO.getRetryable(maxAttempts, maxResults);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FhirSyncStatus> getByStatus(String status, int maxResults) {
        return baseObjectDAO.getByStatus(status, maxResults);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return baseObjectDAO.countByStatus(status);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_ERROR_LENGTH ? value.substring(0, MAX_ERROR_LENGTH) : value;
    }
}
