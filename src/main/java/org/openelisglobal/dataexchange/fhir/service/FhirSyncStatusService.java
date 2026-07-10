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

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirSyncStatus;

public interface FhirSyncStatusService extends BaseObjectService<FhirSyncStatus, String> {

    /**
     * Enregistre (ou récupère) l'événement de suivi pour une transformation, en
     * statut PENDING. Réutilise l'événement existant de même cible+trigger s'il y
     * en a un (évite d'accumuler des lignes pour le même sample re-transformé).
     * Retourne l'id de l'événement.
     */
    String recordPending(String triggerType, String targetType, String targetId);

    /** Marque un événement SUCCESS (efface l'erreur). */
    void markSuccess(String syncStatusId);

    /** Marque un événement FAILED : incrémente attemptCount, stocke le message (tronqué). */
    void markFailed(String syncStatusId, String errorMessage);

    /** Échecs rejouables (FAILED, attemptCount < maxAttempts). */
    List<FhirSyncStatus> getRetryable(int maxAttempts, int maxResults);

    /** Événements par statut (monitoring). */
    List<FhirSyncStatus> getByStatus(String status, int maxResults);

    /** Comptage par statut (résumé monitoring). */
    long countByStatus(String status);
}
