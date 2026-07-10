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

import java.util.Arrays;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirSyncConstants;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirSyncStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rejoue périodiquement les transformations FHIR en échec (FhirSyncStatus =
 * FAILED, attemptCount < max). Rejoue via transformPersistObjectsUnderSamples,
 * qui régénère TOUT le FHIR d'un sample (order/results/validation) — donc un seul
 * point de rejeu couvre les trois déclencheurs pour une cible SAMPLE.
 */
@Component
public class FhirSyncRetryTask {

    @Autowired
    private FhirSyncStatusService fhirSyncStatusService;

    @Autowired
    private FhirTransformService fhirTransformService;

    // Activé par défaut. Désactivable (org.openelisglobal.fhir.retry.enabled=false).
    @Value("${org.openelisglobal.fhir.retry.enabled:true}")
    private boolean retryEnabled;

    // Nombre max de tentatives avant d'abandonner (l'événement reste FAILED mais
    // n'est plus rejoué automatiquement — visible dans le monitoring pour action).
    @Value("${org.openelisglobal.fhir.retry.maxAttempts:5}")
    private int maxAttempts;

    // Nombre d'événements traités par cycle (borne la charge).
    @Value("${org.openelisglobal.fhir.retry.batchSize:20}")
    private int batchSize;

    // Intervalle du job (ms) — 10 min par défaut. initialDelay laisse l'appli
    // démarrer avant le premier passage.
    @Scheduled(fixedDelayString = "${org.openelisglobal.fhir.retry.intervalMs:600000}", initialDelay = 120000)
    public void retryFailedTransformations() {
        if (!retryEnabled) {
            return;
        }
        List<FhirSyncStatus> retryable;
        try {
            retryable = fhirSyncStatusService.getRetryable(maxAttempts, batchSize);
        } catch (Exception e) {
            LogEvent.logError("FhirSyncRetryTask", "retryFailedTransformations", e.toString());
            return;
        }
        if (retryable == null || retryable.isEmpty()) {
            return;
        }
        LogEvent.logInfo("FhirSyncRetryTask", "retryFailedTransformations",
                "Retrying " + retryable.size() + " failed FHIR transformation(s)");
        for (FhirSyncStatus event : retryable) {
            retryOne(event);
        }
    }

    private void retryOne(FhirSyncStatus event) {
        // On ne sait rejouer que les cibles SAMPLE (transformPersistObjectsUnderSamples).
        if (!FhirSyncConstants.TARGET_SAMPLE.equals(event.getTargetType()) || event.getTargetId() == null) {
            return;
        }
        try {
            // Attend le résultat (le Future porte l'exception éventuelle) pour savoir
            // si le rejeu a réellement réussi.
            fhirTransformService.transformPersistObjectsUnderSamples(Arrays.asList(event.getTargetId())).get();
            fhirSyncStatusService.markSuccess(event.getId());
        } catch (Exception e) {
            // Cause racine si disponible (ExecutionException enveloppe l'erreur async).
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fhirSyncStatusService.markFailed(event.getId(), cause.toString());
            LogEvent.logWarn("FhirSyncRetryTask", "retryOne",
                    "Retry failed for sample " + event.getTargetId() + ": " + cause);
        }
    }
}
