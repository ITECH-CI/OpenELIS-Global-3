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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.sync.dto.EorderRequestDTO;
import org.openelisglobal.dataexchange.sync.dto.VlRequestAckDTO;
import org.openelisglobal.dataexchange.sync.dto.VlRequestPullResponse;
import org.openelisglobal.dataexchange.sync.service.EorderImportService.ImportResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pull entrant des demandes de charge virale depuis le serveur consolidé
 * (incrément 4c). Draine périodiquement {@code GET /v1/vl-requests} (pagination
 * par curseur), importe chaque demande en {@code ElectronicOrder} via
 * {@link EorderImportService} (services métier OE, PAS de SQL brut), puis ACK —
 * l'ACK "OK" retire la demande du flux côté SC.
 *
 * <p>
 * Robustesse rapatriée de l'uploader (ce que le moteur natif ne fournit pas) :
 * verrou anti-chevauchement avec vol après timeout, circuit breaker, checkpoint
 * persistant AVANCÉ uniquement quand la page entière a réussi (les échecs sont
 * rejoués), ACK à idempotence stable ({@code eventUuid} dérivé du
 * {@code requestUuid + importStatus} : un retry du même résultat est
 * idempotent, un changement FAILED→OK est retraité). L'idempotence d'import
 * s'appuie sur {@code electronic_order.external_id = requestUuid}.
 */
@Component
public class DataPullTask {

    @Autowired
    private ConsolidatedServerClient client;

    @Autowired
    private EorderImportService importService;

    @Autowired
    private EorderPullCheckpointService checkpointService;

    /** Config admin dynamique (activation/labUuid), relue à chaque tick. */
    @Autowired
    private SyncConfigService config;

    @Value("${org.openelisglobal.consolidated.pull.batchSize:50}")
    private int batchSize;

    @Value("${org.openelisglobal.consolidated.pull.maxPagesPerRun:50}")
    private int maxPagesPerRun;

    private static final long LOCK_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final long CIRCUIT_COOLDOWN_MS = 5 * 60 * 1000L;

    private final AtomicBoolean pullRunning = new AtomicBoolean(false);
    private volatile long pullStartedAt = 0L;
    private volatile int consecutiveFailures = 0;
    private volatile long circuitOpenedAt = 0L;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Scheduled(fixedDelayString = "${org.openelisglobal.consolidated.pull.intervalMs:60000}", initialDelay = 90000)
    public void pullFromConsolidatedServer() {
        if (!config.isPullEnabled()) {
            return;
        }
        if (GenericValidator.isBlankOrNull(config.getLabUuid()) || !client.isConfigured()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "pullFromConsolidatedServer",
                    "pull activé mais labUuid/serveur consolidé non configuré — ignoré");
            return;
        }
        if (isCircuitOpen()) {
            return;
        }
        if (!acquireLock()) {
            return;
        }
        try {
            drain();
            consecutiveFailures = 0;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "pullFromConsolidatedServer", "échec du pull : " + e);
            registerFailure();
        } finally {
            pullRunning.set(false);
            pullStartedAt = 0L;
        }
    }

    private void drain() throws Exception {
        String checkpoint = checkpointService.load();
        int pages = 0;
        boolean hasMore = true;
        while (hasMore && pages < maxPagesPerRun) {
            VlRequestPullResponse response = client.pullRequests(config.getLabUuid(), checkpoint, batchSize);
            if (response == null || response.getRequests() == null || !response.getRequests().isArray()
                    || response.getRequests().isEmpty()) {
                break;
            }
            boolean pageFullySucceeded = true;
            for (JsonNode requestNode : response.getRequests()) {
                if (!processOne(requestNode)) {
                    pageFullySucceeded = false;
                }
            }
            // N'AVANCE le curseur QUE si la page entière a été traitée (import acquis ET
            // ACK confirmé). Sinon on laisse le checkpoint sur place : la page sera
            // re-pullée au prochain run pour rejouer les échecs. Évite qu'une demande
            // en échec passe DERRIÈRE le curseur et ne soit jamais re-servie (le filtre
            // keyset du SC ne la ramènerait plus). Un re-import est neutralisé par
            // l'idempotence external_id ; un re-ACK OK est idempotent (eventUuid stable).
            if (pageFullySucceeded && !GenericValidator.isBlankOrNull(response.getNextCheckpoint())
                    && !response.getNextCheckpoint().equals(checkpoint)) {
                checkpoint = response.getNextCheckpoint();
                checkpointService.save(checkpoint);
            } else if (!pageFullySucceeded) {
                // On arrête le run : rejouer au prochain tick plutôt que boucler sur une
                // page qui échoue (et éviter de marteler le SC).
                break;
            } else {
                // Page OK mais curseur non avancé (nextCheckpoint absent/inchangé) : fin.
                break;
            }
            pages++;
            hasMore = response.getCount() >= batchSize;
        }
    }

    /**
     * Importe une demande puis ACK. Le JSON BRUT du nœud est conservé dans
     * electronic_order.data ; le DTO typé sert au mapping.
     *
     * @return true si l'import est acquis (créé OU déjà présent) ET l'ACK confirmé.
     *         false sur tout échec (import ou ACK) — la page ne sera pas
     *         checkpointée.
     */
    private boolean processOne(JsonNode requestNode) {
        String rawJson = requestNode.toString();
        EorderRequestDTO dto;
        try {
            dto = objectMapper.treeToValue(requestNode, EorderRequestDTO.class);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processOne", "demande illisible, ignorée : " + e);
            return false;
        }
        if (dto == null || GenericValidator.isBlankOrNull(dto.getRequestUuid())) {
            return false;
        }

        ImportResult result = importService.importRequest(dto, rawJson);
        // ACK dans tous les cas où l'import est acquis (créé OU déjà présent) : c'est
        // l'ACK qui retire la demande du flux côté SC (sinon re-livraison en boucle).
        boolean acked = sendAck(dto, result);
        return result.isSuccess() && acked;
    }

    /** @return true si le SC a confirmé l'ACK (2xx). */
    private boolean sendAck(EorderRequestDTO dto, ImportResult result) {
        VlRequestAckDTO ack = new VlRequestAckDTO();
        String importStatus = result.isSuccess() ? "OK" : "FAILED";
        // eventUuid déterministe depuis requestUuid + importStatus. Deux propriétés :
        // (a) un RETRY du MÊME résultat réutilise le même eventUuid -> idempotent côté
        // SC (le SC ignore un eventUuid déjà vu, cf. processAck) ;
        // (b) un CHANGEMENT de résultat (FAILED -> OK au run suivant) produit un
        // eventUuid DIFFÉRENT -> le SC le traite. Sinon (eventUuid dérivé du seul
        // requestUuid) un ACK "OK" après un ACK "FAILED" serait ignoré et la demande
        // resterait FAILED à jamais côté SC.
        ack.setEventUuid(deterministicEventUuid(dto.getRequestUuid(), importStatus));
        ack.setRequestUuid(dto.getRequestUuid());
        ack.setPlatform(config.getLabUuid());
        ack.setImportStatus(importStatus);
        if (!result.isSuccess()) {
            ack.setErrorMessage(result.getErrorMessage());
        }
        try {
            boolean acked = client.ackRequest(ack);
            if (!acked) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "sendAck",
                        "ACK non confirmé (2xx absent) pour " + dto.getRequestUuid());
            }
            return acked;
        } catch (Exception e) {
            // ACK perdu : l'import reste acquis (idempotence external_id). La page ne sera
            // pas checkpointée, donc re-pullée et re-ACKée au prochain cycle. Non bloquant.
            LogEvent.logWarn(this.getClass().getSimpleName(), "sendAck",
                    "échec ACK pour " + dto.getRequestUuid() + " : " + e.getMessage());
            return false;
        }
    }

    /**
     * UUID v3 (déterministe) dérivé de {@code requestUuid + importStatus} — stable
     * pour un retry du même résultat, différent quand le résultat change.
     */
    private static String deterministicEventUuid(String requestUuid, String importStatus) {
        return UUID.nameUUIDFromBytes(("ack:" + requestUuid + ":" + importStatus).getBytes(StandardCharsets.UTF_8))
                .toString();
    }

    // --- Verrou anti-chevauchement + circuit breaker ---------------------------

    private boolean acquireLock() {
        if (pullRunning.compareAndSet(false, true)) {
            pullStartedAt = timeMs();
            return true;
        }
        // Vol du verrou si détenu au-delà du timeout (crash/OOM du run précédent).
        long started = pullStartedAt;
        if (started > 0 && timeMs() - started > LOCK_TIMEOUT_MS) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "acquireLock",
                    "verrou de pull volé après timeout (run précédent bloqué)");
            pullStartedAt = timeMs();
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
            // Cooldown écoulé : on referme le circuit et on retente.
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
