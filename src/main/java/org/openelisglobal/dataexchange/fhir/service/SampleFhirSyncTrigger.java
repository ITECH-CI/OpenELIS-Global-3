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
package org.openelisglobal.dataexchange.fhir.service;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirSyncConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Déclencheur ASYNCHRONE et TRACÉ de la transformation/persistance FHIR d'un
 * bon, par identifiant d'échantillon (sampleId).
 *
 * <p>
 * À utiliser depuis un workflow qui persiste un bon SANS produire le FHIR
 * lui-même (ex. saisie via {@code Accessioner} — charge virale / saisie par
 * projet — qui n'expose pas le {@code SamplePatientUpdateData} nécessaire à
 * l'event {@code SamplePatientUpdateDataCreatedEvent}). La méthode
 * {@link #triggerForSample(String)} est {@code @Async} : l'appelant (le
 * contrôleur de saisie) rend la main IMMÉDIATEMENT — la demande n'est jamais
 * ralentie ni mise en échec par la transformation FHIR, qui est un effet de
 * bord.
 *
 * <p>
 * Le statut est enregistré dans {@code fhir_sync_status} (rejouable via le
 * monitoring / la tâche de retry / le batch {@code /OEToFhir}), exactement
 * comme le fait {@code SampleFhirTransformEventListener} pour la saisie
 * standard. La transformation elle-même relit tout depuis la base et assigne
 * les fhirUuid manquants ; à n'appeler donc qu'APRÈS le commit de la saisie.
 */
@Component
public class SampleFhirSyncTrigger {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private FhirSyncStatusService fhirSyncStatusService;

    /**
     * Transforme/persiste le FHIR du bon {@code sampleId} en arrière-plan, avec
     * traçage {@code ORDER_ENTRY}. Ne lève jamais : toute erreur est tracée
     * ({@code markFailed}) et journalisée, sans remonter à l'appelant.
     *
     * @param sampleId id de l'échantillon (Sample) déjà persisté et committé
     */
    @Async
    public void triggerForSample(String sampleId) {
        if (GenericValidator.isBlankOrNull(sampleId)) {
            return;
        }
        String syncStatusId = null;
        try {
            syncStatusId = fhirSyncStatusService.recordPending(FhirSyncConstants.TRIGGER_ORDER_ENTRY,
                    FhirSyncConstants.TARGET_SAMPLE, sampleId);
            // .get() attend la fin DANS ce thread async (l'appelant, lui, ne bloque
            // pas) — nécessaire pour marquer le statut réel une fois la transfo finie.
            List<String> sampleIds = Arrays.asList(sampleId);
            fhirTransformService.transformPersistObjectsUnderSamples(sampleIds).get();
            if (syncStatusId != null) {
                fhirSyncStatusService.markSuccessWithIssues(syncStatusId,
                        fhirTransformService.collectCompletenessIssuesForSample(sampleId));
            }
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LogEvent.logWarn(this.getClass().getSimpleName(), "triggerForSample",
                    "FHIR sync failed for sample " + sampleId + ": " + cause);
            if (syncStatusId != null) {
                try {
                    fhirSyncStatusService.markFailed(syncStatusId, cause.toString());
                } catch (Exception ignored) {
                    // markFailed est déjà défensif ; on ne relaie rien.
                }
            }
        }
    }
}
