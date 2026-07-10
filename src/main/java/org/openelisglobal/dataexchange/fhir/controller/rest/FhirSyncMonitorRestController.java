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
package org.openelisglobal.dataexchange.fhir.controller.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirSyncConstants;
import org.openelisglobal.dataexchange.fhir.service.FhirSyncStatusService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirSyncStatus;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirSyncStatus.SyncStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API de monitoring des transformations FHIR (voir FhirSyncStatus). */
@RestController
@RequestMapping("/rest/fhir-sync")
public class FhirSyncMonitorRestController {

    @Autowired
    private FhirSyncStatusService fhirSyncStatusService;

    @Autowired
    private org.openelisglobal.sample.service.SampleService sampleService;

    // Résolution paresseuse : FhirTransformService participe à une référence
    // circulaire (fhirTransformServiceImpl <-> fhirReferralServiceImpl) ; l'injecter
    // en champ dans un controller peut empêcher le démarrage du contexte.
    private FhirTransformService fhirTransformService() {
        return org.openelisglobal.spring.util.SpringContext.getBean(FhirTransformService.class);
    }

    private static final java.text.SimpleDateFormat DISPLAY_FORMAT = new java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm");

    // Résout le numéro labo (accession number) depuis le sampleId. Best-effort :
    // renvoie null si le sample n'existe pas (id historique, supprimé…).
    private String resolveAccessionNumber(String targetType, String targetId) {
        if (!FhirSyncConstants.TARGET_SAMPLE.equals(targetType) || targetId == null) {
            return null;
        }
        try {
            org.openelisglobal.sample.valueholder.Sample sample = sampleService.get(targetId);
            return sample != null ? sample.getAccessionNumber() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Compteurs par statut (résumé pour un bandeau/dashboard). */
    @GetMapping("/summary")
    public Map<String, Long> summary() {
        Map<String, Long> counts = new HashMap<>();
        for (SyncStatus s : SyncStatus.values()) {
            counts.put(s.name(), fhirSyncStatusService.countByStatus(s.name()));
        }
        return counts;
    }

    /** Liste des événements d'un statut (défaut FAILED), limitée. */
    @GetMapping("/list")
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "FAILED") String status,
            @RequestParam(defaultValue = "200") int max) {
        List<FhirSyncStatus> items = fhirSyncStatusService.getByStatus(status, max);
        List<Map<String, Object>> out = new ArrayList<>();
        for (FhirSyncStatus item : items) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", item.getId());
            row.put("triggerType", item.getTriggerType());
            row.put("targetType", item.getTargetType());
            row.put("targetId", item.getTargetId());
            // Numéro labo lisible en plus de l'id technique.
            row.put("accessionNumber", resolveAccessionNumber(item.getTargetType(), item.getTargetId()));
            row.put("status", item.getStatus());
            row.put("attemptCount", item.getAttemptCount());
            // Date formatée (jj/mm/aaaa hh:mm) au lieu du timestamp epoch.
            row.put("lastAttemptAt",
                    item.getLastAttemptAt() != null ? DISPLAY_FORMAT.format(item.getLastAttemptAt()) : null);
            row.put("errorMessage", item.getErrorMessage());
            out.add(row);
        }
        return out;
    }

    /**
     * Rejeu historique des samples reçus dans une plage de dates (dateEnd optionnel,
     * défaut = aujourd'hui côté service). Régénère le FHIR complet de chaque sample
     * en PUT idempotent (pas de doublon). Trace chaque sample dans le monitoring.
     * Format de date attendu = format d'affichage local (ex FR : jj/mm/aaaa).
     */
    @PostMapping("/replay")
    public Map<String, Object> replaySince(@RequestParam String dateStart,
            @RequestParam(required = false) String dateEnd) {
        Map<String, Object> result = new HashMap<>();
        List<org.openelisglobal.sample.valueholder.Sample> samples;
        try {
            samples = sampleService.getSamplesReceivedInDateRange(dateStart, dateEnd);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "invalid date range: " + e.getMessage());
            return result;
        }

        int total = samples != null ? samples.size() : 0;
        int success = 0;
        int failed = 0;
        for (org.openelisglobal.sample.valueholder.Sample sample : samples) {
            String sampleId = sample.getId();
            String syncStatusId = null;
            try {
                syncStatusId = fhirSyncStatusService.recordPending(FhirSyncConstants.TRIGGER_VALIDATION,
                        FhirSyncConstants.TARGET_SAMPLE, sampleId);
                fhirTransformService().transformPersistObjectsUnderSamples(Arrays.asList(sampleId)).get();
                if (syncStatusId != null) {
                    fhirSyncStatusService.markSuccess(syncStatusId);
                }
                success++;
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                LogEvent.logWarn("FhirSyncMonitorRestController", "replaySince",
                        "replay failed for sample " + sampleId + ": " + cause);
                if (syncStatusId != null) {
                    fhirSyncStatusService.markFailed(syncStatusId, cause.toString());
                }
                failed++;
            }
        }
        result.put("success", true);
        result.put("total", total);
        result.put("succeeded", success);
        result.put("failed", failed);
        return result;
    }

    /** Rejeu manuel d'un événement (bouton du monitoring). Rejoue le sample cible. */
    @PostMapping("/retry/{id}")
    public Map<String, Object> retry(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        FhirSyncStatus event = fhirSyncStatusService.get(id);
        if (event == null) {
            result.put("success", false);
            result.put("message", "not found");
            return result;
        }
        if (!FhirSyncConstants.TARGET_SAMPLE.equals(event.getTargetType()) || event.getTargetId() == null) {
            result.put("success", false);
            result.put("message", "target not replayable");
            return result;
        }
        try {
            fhirTransformService().transformPersistObjectsUnderSamples(Arrays.asList(event.getTargetId())).get();
            fhirSyncStatusService.markSuccess(id);
            result.put("success", true);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fhirSyncStatusService.markFailed(id, cause.toString());
            LogEvent.logWarn("FhirSyncMonitorRestController", "retry", cause.toString());
            result.put("success", false);
            result.put("message", cause.toString());
        }
        return result;
    }
}
