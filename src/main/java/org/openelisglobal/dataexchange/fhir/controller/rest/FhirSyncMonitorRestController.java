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
    private FhirTransformService fhirTransformService;

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
            row.put("status", item.getStatus());
            row.put("attemptCount", item.getAttemptCount());
            row.put("lastAttemptAt", item.getLastAttemptAt());
            row.put("errorMessage", item.getErrorMessage());
            out.add(row);
        }
        return out;
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
            fhirTransformService.transformPersistObjectsUnderSamples(Arrays.asList(event.getTargetId())).get();
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
