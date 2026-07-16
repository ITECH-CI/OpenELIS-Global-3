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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.sync.dto.VlRequestEventDTO;
import org.openelisglobal.dataexchange.sync.valueholder.EorderSyncStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Push SORTANT des statuts/résultats e-order vers le serveur consolidé
 * (incrément 4d). Draine {@code eorder_sync_status} (sync_flag IN (2,6)),
 * construit l'événement via {@link EorderEventBuilder}, applique
 * l'anti-régression, puis pousse {@code POST /v1/vl-requests/events} et met à
 * jour le suivi.
 *
 * <p>
 * Anti-régression alignée sur le SC (BLOCAGE TOTAL) : on ne pousse pas un
 * statut inférieur au dernier envoyé, SAUF s'il porte un nouveau résultat/date
 * de validation (correction biologiste). Ordre : ACCEPTED &lt; IN_PROGRESS &lt;
 * COMPLETED &lt; RELEASED ; terminaux CANCELLED/REJECTED/FAILED.
 */
@Component
public class DataPushTask {

    @Autowired
    private ConsolidatedServerClient client;

    @Autowired
    private EorderSyncStatusService syncStatusService;

    @Autowired
    private EorderEventBuilder eventBuilder;

    @Value("${org.openelisglobal.consolidated.push.enabled:false}")
    private boolean pushEnabled;

    @Value("${org.openelisglobal.consolidated.push.batchSize:100}")
    private int batchSize;

    @Value("${org.openelisglobal.consolidated.push.inProgressTimeoutMinutes:5}")
    private int inProgressTimeoutMinutes;

    @Value("${org.openelisglobal.consolidated.sync.labUuid:}")
    private String labUuid;

    private static final long LOCK_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final long CIRCUIT_COOLDOWN_MS = 5 * 60 * 1000L;

    private final AtomicBoolean pushRunning = new AtomicBoolean(false);
    private volatile long pushStartedAt = 0L;
    private volatile int consecutiveFailures = 0;
    private volatile long circuitOpenedAt = 0L;

    @Scheduled(fixedDelayString = "${org.openelisglobal.consolidated.push.intervalMs:60000}", initialDelay = 150000)
    public void pushStatusesToConsolidatedServer() {
        if (!pushEnabled) {
            return;
        }
        if (GenericValidator.isBlankOrNull(labUuid) || !client.isConfigured()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "pushStatusesToConsolidatedServer",
                    "push activé mais labUuid/serveur consolidé non configuré — ignoré");
            return;
        }
        if (isCircuitOpen() || !acquireLock()) {
            return;
        }
        try {
            // Réarme les lignes IN_PROGRESS orphelines (crash/restart entre
            // markInProgress et markSynced) avant de drainer — sinon elles resteraient
            // coincées en 4 (jamais reprises par le batch 2/6, jamais réarmées).
            try {
                int reset = syncStatusService.resetStaleInProgress(inProgressTimeoutMinutes);
                if (reset > 0) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "pushStatusesToConsolidatedServer",
                            reset + " ligne(s) push IN_PROGRESS orpheline(s) réarmée(s)");
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "pushStatusesToConsolidatedServer",
                        "échec reset stale : " + e);
            }

            List<EorderSyncStatus> batch = syncStatusService.getBatchToPush(batchSize);
            if (batch == null || batch.isEmpty()) {
                return;
            }
            boolean anyFailure = false;
            for (EorderSyncStatus sync : batch) {
                if (!processOne(sync)) {
                    anyFailure = true;
                }
            }
            if (anyFailure) {
                registerFailure();
            } else {
                consecutiveFailures = 0;
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "pushStatusesToConsolidatedServer",
                    "échec du push : " + e);
            registerFailure();
        } finally {
            pushRunning.set(false);
            pushStartedAt = 0L;
        }
    }

    /**
     * @return true si la ligne a été traitée (poussée OU volontairement purgée).
     */
    private boolean processOne(EorderSyncStatus sync) {
        syncStatusService.markInProgress(sync.getId());
        try {
            VlRequestEventDTO event = eventBuilder.build(sync);
            if (event == null || GenericValidator.isBlankOrNull(event.getLabStatus())) {
                // Analyse non résoluble / pas de statut : on purge (SYNCED) pour ne pas
                // boucler sur une ligne inexploitable.
                syncStatusService.markSynced(sync.getId(), sync.getLastSentLabStatus(), sync.getLastSentResult(),
                        sync.getLabno(), sync.getLastEventUuid());
                return true;
            }

            // Anti-régression : ALIGNÉE EXACTEMENT sur le SC — blocage INCONDITIONNEL
            // d'une régression de statut. Le SC (processEvent) rejette EN BLOC toute
            // régression (RELEASED→COMPLETED…) SANS exception pour résultat/date, mais
            // répond quand même HTTP 200 : si on poussait, on marquerait SYNCED un statut
            // que le SC a ignoré (désync silencieuse). Une correction biologiste n'est PAS
            // une régression (statut RELEASED inchangé, seul le résultat change) → elle
            // passe par la garde "rien de nouveau" ci-dessous, pas par ici.
            if (isRegression(sync.getLastSentLabStatus(), event.getLabStatus())) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processOne", "régression de statut ignorée pour "
                        + sync.getRequestUuid() + " : " + sync.getLastSentLabStatus() + " -> " + event.getLabStatus());
                syncStatusService.markSynced(sync.getId(), sync.getLastSentLabStatus(), sync.getLastSentResult(),
                        sync.getLabno(), sync.getLastEventUuid());
                return true;
            }

            // Rien de nouveau à envoyer (même statut ET même résultat déjà confirmés) :
            // on purge sans POST inutile (le SC serait de toute façon idempotent).
            boolean sameStatus = equalsNullable(event.getLabStatus(), sync.getLastSentLabStatus());
            boolean sameResult = equalsNullable(event.getTestResult(), sync.getLastSentResult());
            if (sameStatus && sameResult) {
                syncStatusService.markSynced(sync.getId(), sync.getLastSentLabStatus(), sync.getLastSentResult(),
                        sync.getLabno(), sync.getLastEventUuid());
                return true;
            }

            boolean pushed = client.pushEvent(event);
            if (pushed) {
                syncStatusService.markSynced(sync.getId(), event.getLabStatus(), event.getTestResult(),
                        event.getLabno(), event.getEventUuid());
                return true;
            }
            syncStatusService.markSendFailed(sync.getId());
            return false;
        } catch (Exception e) {
            syncStatusService.markToUpdate(sync.getId());
            LogEvent.logError(this.getClass().getSimpleName(), "processOne",
                    "échec push statut " + sync.getRequestUuid() + " : " + e);
            return false;
        }
    }

    // --- Anti-régression (aligné SC) -------------------------------------------

    private static final List<String> TERMINALS = Arrays.asList("CANCELLED", "REJECTED", "FAILED");

    /** Ordre du parcours normal ; -1 pour terminaux/inconnus. */
    private static int ordinal(String labStatus) {
        if (labStatus == null) {
            return -1;
        }
        switch (labStatus) {
        case "ACCEPTED":
            return 1;
        case "IN_PROGRESS":
            return 2;
        case "COMPLETED":
            return 3;
        case "RELEASED":
            return 4;
        default:
            return -1;
        }
    }

    private static boolean isRegression(String lastSent, String next) {
        if (lastSent == null || next == null || lastSent.equals(next)) {
            return false;
        }
        if (TERMINALS.contains(lastSent)) {
            // Un terminal ne peut être remplacé que par un terminal.
            return !TERMINALS.contains(next);
        }
        int lastOrd = ordinal(lastSent);
        int nextOrd = ordinal(next);
        return lastOrd > 0 && nextOrd > 0 && nextOrd < lastOrd;
    }

    private static boolean equalsNullable(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    // --- Verrou + circuit breaker (identique au pull) --------------------------

    private boolean acquireLock() {
        if (pushRunning.compareAndSet(false, true)) {
            pushStartedAt = timeMs();
            return true;
        }
        long started = pushStartedAt;
        if (started > 0 && timeMs() - started > LOCK_TIMEOUT_MS) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "acquireLock",
                    "verrou de push volé après timeout (run précédent bloqué)");
            pushStartedAt = timeMs();
            return true;
        }
        return false;
    }

    private void registerFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            circuitOpenedAt = timeMs();
            LogEvent.logWarn(this.getClass().getSimpleName(), "registerFailure",
                    "circuit breaker ouvert après " + consecutiveFailures + " échecs consécutifs");
        }
    }

    private boolean isCircuitOpen() {
        if (circuitOpenedAt == 0L) {
            return false;
        }
        if (timeMs() - circuitOpenedAt > CIRCUIT_COOLDOWN_MS) {
            circuitOpenedAt = 0L;
            consecutiveFailures = 0;
            return false;
        }
        return true;
    }

    private static long timeMs() {
        return System.currentTimeMillis();
    }
}
