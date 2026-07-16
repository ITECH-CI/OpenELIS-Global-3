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

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.sync.dto.DataSyncPayload;
import org.openelisglobal.dataexchange.sync.valueholder.AnalysisSyncStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Remontée consolidée SORTANTE (module d'échange unifié, incrément 4b). Draine
 * périodiquement la file CDC {@code analysis_sync_status} : les analyses
 * modifiées (flag 1/2) sont assemblées en payload JSON (via services métier) et
 * POSTées au serveur consolidé, puis marquées UP_TO_DATE (ou re-mises en file
 * en cas d'échec).
 *
 * <p>
 * Portage modernisé de {@code DataSyncServiceImpl} (oedatauploader), avec les
 * corrections : transitions d'état via un service {@code @Transactional} PUBLIC
 * (le proxy s'applique — le bug des méthodes privées de l'uploader est corrigé)
 * ; activation par feature flag (comme {@code FhirSyncRetryTask}) ; payload
 * construit depuis les services OE au lieu du SQL en dur ; contrat SC préservé.
 */
@Component
public class DataSyncTask {

    @Autowired
    private AnalysisSyncStatusService analysisSyncStatusService;

    @Autowired
    private DataSyncPayloadBuilder payloadBuilder;

    @Autowired
    private ConsolidatedServerClient consolidatedServerClient;

    // Désactivé par défaut : la remontée consolidée n'est active que si un serveur
    // consolidé est configuré ET le flag positionné (évite tout POST intempestif en
    // dev). Miroir du patron FhirSyncRetryTask.
    @Value("${org.openelisglobal.consolidated.sync.enabled:false}")
    private boolean syncEnabled;

    // Taille de lot (= nombre d'analyses remontées par itération de la boucle).
    @Value("${org.openelisglobal.consolidated.sync.batchSize:100}")
    private int batchSize;

    // Délai (min) au-delà duquel une ligne IN_PROGRESS est considérée orpheline
    // (crash/restart) et remise en file.
    @Value("${org.openelisglobal.consolidated.sync.inProgressTimeoutMinutes:5}")
    private int inProgressTimeoutMinutes;

    // Borne de sécurité sur le nombre d'itérations par tick (évite une boucle qui
    // draine indéfiniment si la file se remplit aussi vite qu'elle se vide).
    @Value("${org.openelisglobal.consolidated.sync.maxBatchesPerRun:50}")
    private int maxBatchesPerRun;

    /**
     * Tick d'ordonnancement (5 min par défaut, initialDelay laisse démarrer
     * l'appli). fixedDelay : compte à partir de la FIN de l'exécution précédente →
     * jamais deux runs concurrents (la seule protection concurrentielle, comme
     * l'uploader).
     */
    @Scheduled(fixedDelayString = "${org.openelisglobal.consolidated.sync.intervalMs:300000}", initialDelay = 120000)
    public void syncToConsolidatedServer() {
        if (!syncEnabled) {
            return;
        }
        if (!consolidatedServerClient.isConfigured()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncToConsolidatedServer",
                    "remontée consolidée activée mais serveur consolidé non configuré (apiUrl/authUrl) — ignoré");
            return;
        }

        // 1) Récupération des lignes orphelines (crash/restart) avant de drainer.
        try {
            int reset = analysisSyncStatusService.resetStaleInProgress(inProgressTimeoutMinutes);
            if (reset > 0) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "syncToConsolidatedServer",
                        reset + " ligne(s) IN_PROGRESS orpheline(s) réinitialisée(s)");
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncToConsolidatedServer",
                    "échec du reset des lignes orphelines : " + e);
            // non bloquant : on tente quand même de drainer.
        }

        // 2) Drainage par lots.
        int batches = 0;
        while (batches < maxBatchesPerRun) {
            List<AnalysisSyncStatus> batch;
            try {
                batch = analysisSyncStatusService.getBatchToSync(batchSize);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "syncToConsolidatedServer",
                        "échec de sélection du lot : " + e);
                return;
            }
            if (batch == null || batch.isEmpty()) {
                break;
            }
            boolean ok = processBatch(batch);
            batches++;
            if (!ok) {
                // Un échec casse la boucle du tick (le lot est déjà remis en file) ; le
                // prochain tick reprendra. Évite de marteler le SC en erreur.
                break;
            }
        }
    }

    /**
     * Traite un lot : mark IN_PROGRESS → build payload → POST → mark UP_TO_DATE (ou
     * mark TO_UPDATE en cas d'échec). Renvoie true si le lot a été remonté.
     */
    private boolean processBatch(List<AnalysisSyncStatus> batch) {
        List<String> ids = batch.stream().map(AnalysisSyncStatus::getId).collect(Collectors.toList());
        List<String> analysisIds = batch.stream().map(AnalysisSyncStatus::getAnalysisId).filter(a -> a != null)
                .collect(Collectors.toList());

        analysisSyncStatusService.markInProgress(ids);
        try {
            DataSyncPayload payload = payloadBuilder.buildForAnalysisIds(analysisIds);
            boolean acked = consolidatedServerClient.sendPayload(payload);
            if (acked) {
                analysisSyncStatusService.markUpToDate(ids);
                LogEvent.logInfo(this.getClass().getSimpleName(), "processBatch",
                        "lot remonté (" + ids.size() + " analyses)");
                return true;
            }
            analysisSyncStatusService.markToUpdate(ids);
            return false;
        } catch (Exception e) {
            analysisSyncStatusService.markToUpdate(ids);
            LogEvent.logError(this.getClass().getSimpleName(), "processBatch", "échec de remontée du lot : " + e);
            return false;
        }
    }
}
