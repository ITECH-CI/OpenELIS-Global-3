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
package org.openelisglobal.dataexchange.sync.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.sync.dto.VlRequestEventDTO;
import org.openelisglobal.dataexchange.sync.valueholder.EorderSyncStatus;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Construit un {@link VlRequestEventDTO} pour une ligne
 * {@link EorderSyncStatus} (incrément 4d), à partir des SERVICES MÉTIER OE. Le
 * mapping statut OE→labStatus se fait par ENUM {@code AnalysisStatus} (robuste,
 * indépendant de la locale), PAS par comparaison de libellés comme l'uploader.
 */
@Service
public class EorderEventBuilder {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private IStatusService statusService;

    /** Unité de résultat CV (le SC l'attend ; l'uploader la mettait en dur). */
    private static final String VL_RESULT_UNIT = "copies/mL";

    /**
     * Construit l'événement pour la ligne de suivi.
     * {@code @Transactional(readOnly)} : traverse des ValueHolder lazy
     * (analysis→sampleItem→sample). Renvoie null si l'analyse n'est pas résoluble
     * (le caller marquera SYNCED pour purger la file).
     */
    @Transactional(readOnly = true)
    public VlRequestEventDTO build(EorderSyncStatus sync) {
        if (sync == null || GenericValidator.isBlankOrNull(sync.getAnalysisId())) {
            return null;
        }
        Analysis analysis = analysisService.get(sync.getAnalysisId());
        if (analysis == null) {
            return null;
        }

        VlRequestEventDTO event = new VlRequestEventDTO();
        event.setRequestUuid(sync.getRequestUuid());

        String labStatus = mapStatus(analysis);
        event.setLabStatus(labStatus);
        event.setCompletedDate(toLdt(analysis.getCompletedDate()));
        if ("RELEASED".equals(labStatus)) {
            event.setReleasedDate(toLdt(analysis.getReleasedDate()));
        }

        // Labno + received date depuis le sample.
        SampleItem sampleItem = analysis.getSampleItem();
        Sample sample = sampleItem != null ? sampleItem.getSample() : null;
        if (sample != null) {
            event.setLabno(sample.getAccessionNumber());
            event.setReceivedDate(toLdt(sample.getReceivedTimestamp()));
        }

        // Résultat (premier reportable) — seulement si l'analyse est validée.
        if ("COMPLETED".equals(labStatus) || "RELEASED".equals(labStatus)) {
            List<Result> results = resultService.getReportableResultsByAnalysis(analysis);
            if (results == null || results.isEmpty()) {
                results = resultService.getResultsByAnalysis(analysis);
            }
            if (results != null && !results.isEmpty()) {
                Result r = results.get(0);
                String value = safeResultValue(r);
                event.setTestResult(value);
                event.setTestResultInt(parseResultInt(value));
                event.setResultUnit(VL_RESULT_UNIT);
            }
        }

        event.setEventType(
                "RELEASED".equals(labStatus) && event.getTestResult() != null ? "RESULT_POSTED" : "STATUS_UPDATE");
        // eventUuid déterministe : stable pour un même changement (rejeu = no-op côté
        // SC), différent quand statut/résultat/date change (le SC ne traite un nouvel
        // événement que si eventUuid inédit).
        event.setEventUuid(deterministicEventUuid(sync.getRequestUuid(), labStatus, event.getTestResult(),
                event.getReleasedDate(), event.getCompletedDate()));
        return event;
    }

    /**
     * Mapping OE→SC par ENUM. Les statuts de REJET/ANNULATION sont évalués EN
     * PREMIER (avant la priorité aux dates) : une analyse d'abord libérée
     * (released_date posé) puis re-rejetée par le biologiste conserve son
     * released_date ; sans cette priorité, on remonterait RELEASED au lieu de
     * REJECTED. Ensuite : dates (released/completed), puis statut nominal.
     */
    private String mapStatus(Analysis analysis) {
        String statusId = analysis.getStatusId();
        if (!GenericValidator.isBlankOrNull(statusId)) {
            if (statusService.matches(statusId, AnalysisStatus.BiologistRejected)
                    || statusService.matches(statusId, AnalysisStatus.SampleRejected)) {
                return "REJECTED";
            }
            if (statusService.matches(statusId, AnalysisStatus.Canceled)) {
                return "CANCELLED";
            }
            if (statusService.matches(statusId, AnalysisStatus.TechnicalRejected)) {
                return "FAILED";
            }
        }
        if (analysis.getReleasedDate() != null) {
            return "RELEASED";
        }
        if (analysis.getCompletedDate() != null) {
            return "COMPLETED";
        }
        if (GenericValidator.isBlankOrNull(statusId)) {
            return "IN_PROGRESS";
        }
        if (statusService.matches(statusId, AnalysisStatus.Finalized)) {
            return "RELEASED";
        }
        if (statusService.matches(statusId, AnalysisStatus.TechnicalAcceptance)) {
            return "COMPLETED";
        }
        return "IN_PROGRESS";
    }

    private String safeResultValue(Result r) {
        try {
            return resultService.getResultValue(r, false);
        } catch (RuntimeException e) {
            return r.getValue();
        }
    }

    /** Nettoie {@code < > , espaces} et parse en Long ; null si non numérique. */
    private static Long parseResultInt(String value) {
        if (GenericValidator.isBlankOrNull(value)) {
            return null;
        }
        String cleaned = value.replaceAll("[<>,\\s]", "");
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * UUID v3 déterministe = hash de tout le contenu variable de l'événement
     * (requestUuid + labStatus + result + releasedDate + completedDate). Stable
     * pour un même changement (rejeu = no-op côté SC), différent dès qu'un champ
     * change (sinon le SC, idempotent sur eventUuid, ignorerait le nouveau
     * contenu).
     */
    private static String deterministicEventUuid(String requestUuid, String labStatus, String result,
            LocalDateTime releasedDate, LocalDateTime completedDate) {
        String seed = "evt:" + requestUuid + ":" + labStatus + ":" + (result == null ? "" : result) + ":"
                + (releasedDate == null ? "" : releasedDate.toString()) + ":"
                + (completedDate == null ? "" : completedDate.toString());
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static LocalDateTime toLdt(Date d) {
        return d == null ? null : d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
