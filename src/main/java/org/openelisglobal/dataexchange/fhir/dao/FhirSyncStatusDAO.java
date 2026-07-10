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
package org.openelisglobal.dataexchange.fhir.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirSyncStatus;

public interface FhirSyncStatusDAO extends BaseDAO<FhirSyncStatus, String> {

    /** Événements dans un statut donné (SUCCESS/FAILED/PENDING), les plus anciens d'abord. */
    List<FhirSyncStatus> getByStatus(String status, int maxResults);

    /** Échecs rejouables : status=FAILED et attempt_count < maxAttempts, plus anciens d'abord. */
    List<FhirSyncStatus> getRetryable(int maxAttempts, int maxResults);

    /** Dernier événement enregistré pour une cible (dédup / mise à jour d'état). */
    FhirSyncStatus findLatestByTarget(String targetType, String targetId, String triggerType);

    /** Comptage par statut (pour un résumé de monitoring). */
    long countByStatus(String status);
}
