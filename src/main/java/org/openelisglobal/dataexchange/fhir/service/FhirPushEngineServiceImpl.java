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

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Resource;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirPushTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Moteur de PUSH FHIR NATIF (remplace le module {@code dataexport}).
 *
 * <p>
 * À chaque tick, pour chaque cible {@link FhirPushTarget} active dont
 * l'intervalle est écoulé : lit le delta du store FHIR LOCAL par
 * {@code _lastUpdated} (borne basse exclusive = {@code lastPushed} de la cible,
 * borne haute inclusive = début du tick), assemble un Bundle transaction (PUT
 * idempotent par ressource) et le POST vers l'endpoint distant avec
 * l'authentification propre à la cible. En cas de succès, avance le point de
 * reprise ({@code lastPushed = borne haute}).
 *
 * <p>
 * Conçu pour un déploiement LOCAL (pas d'IP publique) : le push est un tirage
 * périodique SORTANT depuis OE, sans dépendre d'une {@code Subscription} FHIR
 * REST-hook (que le serveur distant ne pourrait de toute façon pas rappeler sur
 * une IP privée). Il remplace le moteur dataexport à source identique (le store
 * FHIR local) ; seul le stockage du checkpoint change ({@code fhir_push_target}
 * au lieu de {@code data_export_attempt}).
 *
 * <p>
 * Robustesse (patron aligné sur {@code DataPushTask}) : un seul tick à la fois
 * (verrou avec vol-timeout), circuit breaker après échecs répétés, chaque cible
 * isolée (l'échec de l'une n'empêche pas les autres). La borne haute de la
 * fenêtre est FIGÉE au début du traitement de chaque cible pour ne jamais
 * sauter une ressource créée pendant le push.
 */
@Service
public class FhirPushEngineServiceImpl {

    // Ressources poussées par défaut si une cible n'en précise aucune.
    private static final List<String> DEFAULT_RESOURCES = Arrays.asList("Task", "Patient", "ServiceRequest",
            "DiagnosticReport", "Observation", "Specimen", "Practitioner", "Encounter");

    private static final int DEFAULT_INTERVAL_MINUTES = 5;
    private static final int PAGE_SIZE = 200;

    // Taille max d'un Bundle transaction envoyé au distant. Le premier push d'une
    // cible (lastPushed=null) rapatrie tout l'historique : sans découpage, un
    // Bundle
    // de plusieurs milliers de ressources dépasserait les limites du serveur
    // distant. On envoie par lots ; le checkpoint n'avance qu'après le DERNIER lot.
    private static final int TRANSACTION_CHUNK_SIZE = 200;

    // Marge retranchée à la borne haute de la fenêtre _lastUpdated, pour absorber
    // la
    // dérive d'horloge OE↔HAPI local + la latence d'indexation du store (le store
    // est un processus HAPI séparé). Évite de « sauter » une ressource écrite juste
    // avant le tick mais visible après. Ces ressources sont reprises au tick
    // suivant.
    private static final long LAG_SAFETY_SECONDS = 30L;

    private static final long LOCK_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final long CIRCUIT_COOLDOWN_MS = 5 * 60 * 1000L;

    @Autowired
    private FhirPushTargetService fhirPushTargetService;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private FhirConfig fhirConfig;

    /**
     * Interrupteur maître : le moteur ne pousse que si activé. Désactivé par défaut
     * (aucune cible poussée tant qu'un admin ne l'active pas ET n'a pas déclaré au
     * moins une cible).
     */
    @Value("${org.openelisglobal.fhir.push.enabled:false}")
    private boolean pushEnabled;

    /**
     * Autorise un endpoint de cible en HTTP CLAIR (http://). Par défaut false : le
     * moteur force https:// avant tout POST — sinon le secret d'auth (Bearer/Basic)
     * transiterait en clair. À ne mettre true qu'en dev/local. Même garde-fou que
     * le module sync ({@code consolidated.sync.allowHTTP}).
     */
    @Value("${org.openelisglobal.fhir.push.allowHTTP:false}")
    private boolean allowHttp;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long startedAt = 0L;
    private volatile int consecutiveFailures = 0;
    private volatile long circuitOpenedAt = 0L;

    @Scheduled(fixedDelayString = "${org.openelisglobal.fhir.push.intervalMs:60000}", initialDelay = 120000)
    public void pushToTargets() {
        if (!pushEnabled) {
            return;
        }
        if (GenericValidator.isBlankOrNull(fhirConfig.getLocalFhirStorePath())) {
            // Sans store FHIR local, il n'y a rien à lire — inutile de tenter.
            return;
        }
        if (isCircuitOpen() || !acquireLock()) {
            return;
        }
        try {
            List<FhirPushTarget> targets = fhirPushTargetService.getActiveTargets();
            if (targets == null || targets.isEmpty()) {
                consecutiveFailures = 0;
                return;
            }
            IGenericClient localClient = fhirUtil.getLocalFhirClient();
            boolean anyFailure = false;
            for (FhirPushTarget target : targets) {
                if (!isDue(target)) {
                    continue;
                }
                if (!pushOneTarget(localClient, target)) {
                    anyFailure = true;
                }
            }
            if (anyFailure) {
                registerFailure();
            } else {
                consecutiveFailures = 0;
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "pushToTargets", "échec du push FHIR natif : " + e);
            registerFailure();
        } finally {
            running.set(false);
            startedAt = 0L;
        }
    }

    /**
     * Pousse le delta d'UNE cible. Retourne true si le tick s'est déroulé sans
     * erreur (y compris « rien à pousser »), false sur échec (réseau, transaction
     * distante) — l'échec n'avance PAS le point de reprise, donc la fenêtre sera
     * rejouée au prochain tick.
     */
    private boolean pushOneTarget(IGenericClient localClient, FhirPushTarget target) {
        // Borne haute figée maintenant, MOINS une marge de sécurité : le store FHIR
        // local est un processus HAPI SÉPARÉ, dont l'horloge peut dériver de celle
        // d'OE et dont l'indexation (Lucene/DB) est asynchrone. Sans marge, une
        // ressource tamponnée _lastUpdated juste avant le tick mais visible seulement
        // après, avec une horloge HAPI en retard sur OE, passerait SOUS la borne
        // basse (gt lastPushed) du tick suivant → perte silencieuse. On recule donc
        // windowEnd de LAG_SAFETY_SECONDS ; ces ressources seront reprises au tick
        // d'après (léger retard, jamais de perte).
        Date windowEnd = Date.from(Instant.now().minusSeconds(LAG_SAFETY_SECONDS));
        Timestamp windowStart = target.getLastPushed(); // null = tout l'historique

        // markPushAttempt DANS le try : c'est une transaction (merge+flush) qui peut
        // lever (ex. OptimisticLockException si un admin édite la cible pendant le
        // tick). Hors du try, l'exception remonterait et sauterait les cibles
        // suivantes du tick — ce qui casserait l'isolation par-cible.
        try {
            fhirPushTargetService.markPushAttempt(target.getId());

            List<Resource> delta = readLocalDelta(localClient, resolveResources(target), windowStart, windowEnd);
            if (delta.isEmpty()) {
                // Rien de nouveau : on avance quand même le point de reprise pour ne pas
                // re-scanner indéfiniment la même fenêtre vide.
                fhirPushTargetService.markPushSuccess(target.getId(), new Timestamp(windowEnd.getTime()));
                return true;
            }

            IGenericClient remoteClient = buildRemoteClient(target);
            // Envoi par lots (le 1er push d'une cible rapatrie tout l'historique).
            // Le checkpoint n'est avancé qu'APRÈS le dernier lot réussi : si un lot
            // échoue, l'exception remonte, le checkpoint reste, la fenêtre est rejouée
            // (les lots déjà envoyés sont des PUT idempotents → pas de doublon).
            for (int from = 0; from < delta.size(); from += TRANSACTION_CHUNK_SIZE) {
                int to = Math.min(from + TRANSACTION_CHUNK_SIZE, delta.size());
                Bundle transaction = buildTransactionBundle(delta.subList(from, to));
                remoteClient.transaction().withBundle(transaction).execute();
            }

            fhirPushTargetService.markPushSuccess(target.getId(), new Timestamp(windowEnd.getTime()));
            LogEvent.logInfo(this.getClass().getSimpleName(), "pushOneTarget",
                    "poussé " + delta.size() + " ressource(s) vers " + target.getEndpoint());
            return true;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "pushOneTarget",
                    "échec push vers " + target.getEndpoint() + " : " + e);
            return false;
        }
    }

    /**
     * Lit toutes les ressources des types demandés modifiées dans la fenêtre
     * {@code (windowStart, windowEnd]} sur le store local, en suivant la pagination
     * FHIR. Borne basse EXCLUSIVE ({@code gt}) pour ne pas re-pousser la dernière
     * ressource déjà envoyée ; borne haute INCLUSIVE ({@code le}).
     */
    private List<Resource> readLocalDelta(IGenericClient localClient, List<String> resourceTypes, Timestamp windowStart,
            Date windowEnd) {
        DateRangeParam dateRange = new DateRangeParam();
        if (windowStart != null) {
            dateRange.setLowerBound(new DateParam(ParamPrefixEnum.GREATERTHAN, new Date(windowStart.getTime())));
        }
        dateRange.setUpperBound(new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, windowEnd));

        List<Resource> collected = new ArrayList<>();
        for (String resourceType : resourceTypes) {
            Bundle bundle = localClient.search().forResource(resourceType).lastUpdated(dateRange).count(PAGE_SIZE)
                    .returnBundle(Bundle.class).execute();
            collectEntries(bundle, collected);
            while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                bundle = localClient.loadPage().next(bundle).execute();
                collectEntries(bundle, collected);
            }
        }
        return collected;
    }

    private void collectEntries(Bundle bundle, List<Resource> collected) {
        for (BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof Resource) {
                collected.add((Resource) entry.getResource());
            }
        }
    }

    /**
     * Bundle transaction : un PUT par ressource (upsert idempotent par
     * {@code Type/id}) — rejouer la même fenêtre ne crée pas de doublon côté
     * distant.
     */
    private Bundle buildTransactionBundle(List<Resource> resources) {
        Bundle bundle = new Bundle();
        bundle.setType(BundleType.TRANSACTION);
        for (Resource resource : resources) {
            String reference = resource.getResourceType().name() + "/" + resource.getIdElement().getIdPart();
            BundleEntryComponent entry = new BundleEntryComponent();
            entry.setResource(resource);
            entry.setFullUrl(reference);
            entry.setRequest(new BundleEntryRequestComponent().setMethod(HTTPVerb.PUT).setUrl(reference));
            bundle.addEntry(entry);
        }
        return bundle;
    }

    /**
     * Client FHIR distant avec l'authentification propre à la cible. L'endpoint est
     * forcé en HTTPS (sauf allowHttp) AVANT toute pose de credentials : le secret
     * ne doit jamais partir en clair.
     */
    private IGenericClient buildRemoteClient(FhirPushTarget target) {
        String endpoint = enforceHttps(target.getEndpoint());
        String authType = target.getAuthType();
        if ("TOKEN".equalsIgnoreCase(authType) && !GenericValidator.isBlankOrNull(target.getAuthSecret())) {
            return fhirUtil.getFhirClient(endpoint, target.getAuthSecret());
        }
        if ("BASIC".equalsIgnoreCase(authType) && !GenericValidator.isBlankOrNull(target.getAuthUsername())) {
            return fhirUtil.getFhirClient(endpoint, target.getAuthUsername(),
                    target.getAuthSecret() == null ? "" : target.getAuthSecret());
        }
        // BASIC sans utilisateur : l'auth est incomplète. On journalise explicitement
        // (sinon panne muette : POST sans Authorization → 401 en boucle, checkpoint
        // qui n'avance pas, difficile à diagnostiquer).
        if ("BASIC".equalsIgnoreCase(authType)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "buildRemoteClient",
                    "cible " + target.getEndpoint() + " : auth BASIC sans utilisateur — push SANS authentification");
        }
        // NONE (ou auth incomplète) : client nu, sans les credentials globaux
        // fhirstore.* (qui visent le store local, pas ce serveur tiers).
        return fhirUtil.getFhirClientNoAuth(endpoint);
    }

    /**
     * Force HTTPS sur l'endpoint d'une cible (sauf {@code allowHttp=true}) : les
     * credentials d'auth ne doivent jamais transiter en clair. Aligné sur
     * {@code ConsolidatedServerClient.enforceHttps}.
     */
    private String enforceHttps(String url) {
        if (GenericValidator.isBlankOrNull(url)) {
            return url;
        }
        if (allowHttp && url.startsWith("http://")) {
            return url; // HTTP clair explicitement autorisé (dev/local).
        }
        if (url.startsWith("http://")) {
            return "https://" + url.substring("http://".length());
        }
        if (!url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }

    private List<String> resolveResources(FhirPushTarget target) {
        if (GenericValidator.isBlankOrNull(target.getAllowedResources())) {
            return DEFAULT_RESOURCES;
        }
        List<String> names = new ArrayList<>();
        for (String r : target.getAllowedResources().split(",")) {
            if (!r.trim().isEmpty()) {
                names.add(r.trim());
            }
        }
        return names.isEmpty() ? DEFAULT_RESOURCES : names;
    }

    /** Intervalle de la cible écoulé depuis le dernier essai ? */
    private boolean isDue(FhirPushTarget target) {
        Timestamp lastAttempt = target.getLastAttempt();
        if (lastAttempt == null) {
            return true;
        }
        int minutes = target.getIntervalMinutes() != null && target.getIntervalMinutes() > 0
                ? target.getIntervalMinutes()
                : DEFAULT_INTERVAL_MINUTES;
        long dueAt = lastAttempt.getTime() + minutes * 60_000L;
        return System.currentTimeMillis() >= dueAt;
    }

    // --- Verrou + circuit breaker (patron DataPushTask) ------------------------

    private boolean acquireLock() {
        if (running.compareAndSet(false, true)) {
            startedAt = System.currentTimeMillis();
            return true;
        }
        // Vol du verrou si un tick précédent est resté bloqué au-delà du timeout.
        if (startedAt > 0 && System.currentTimeMillis() - startedAt > LOCK_TIMEOUT_MS) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "acquireLock",
                    "verrou push FHIR volé (tick précédent > " + (LOCK_TIMEOUT_MS / 1000) + "s) — reprise");
            startedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private boolean isCircuitOpen() {
        if (consecutiveFailures < MAX_CONSECUTIVE_FAILURES) {
            return false;
        }
        if (System.currentTimeMillis() - circuitOpenedAt >= CIRCUIT_COOLDOWN_MS) {
            // Fin du cooldown : on réarme (demi-ouvert) pour retenter.
            consecutiveFailures = 0;
            return false;
        }
        return true;
    }

    private void registerFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            circuitOpenedAt = System.currentTimeMillis();
            LogEvent.logWarn(this.getClass().getSimpleName(), "registerFailure", "circuit ouvert après "
                    + consecutiveFailures + " échecs consécutifs — pause " + (CIRCUIT_COOLDOWN_MS / 1000) + "s");
        }
    }
}
