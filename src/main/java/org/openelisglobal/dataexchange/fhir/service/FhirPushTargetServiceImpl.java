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

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.ResourceType;
import org.itech.fhir.dataexport.core.model.DataExportTask;
import org.itech.fhir.dataexport.core.service.DataExportTaskService;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.fhir.dao.FhirPushTargetDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirPushTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FhirPushTargetServiceImpl extends AuditableBaseObjectServiceImpl<FhirPushTarget, String>
        implements FhirPushTargetService {

    @Autowired
    protected FhirPushTargetDAO baseObjectDAO;

    @Autowired
    private DataExportTaskService dataExportTaskService;

    // Ressources poussées par défaut si une cible n'en précise aucune. Aligné sur
    // la
    // liste native (fhir.subscriber.resources).
    private static final List<String> DEFAULT_RESOURCES = List.of("Task", "Patient", "ServiceRequest",
            "DiagnosticReport", "Observation", "Specimen", "Practitioner", "Encounter");

    private static final int DEFAULT_INTERVAL_MINUTES = 5;
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    // Marqueur d'appartenance posé dans les headers du DataExportTask : distingue
    // les
    // tâches gérées par cet écran des tâches natives (fhir.subscriber). Transmis
    // comme
    // header à chaque requête vers le distant (inoffensif) et sert de garde-fou
    // pour
    // ne jamais écraser/supprimer une tâche native partageant le même endpoint.
    private static final String MANAGED_HEADER = "X-OE-Managed-By";
    private static final String MANAGED_VALUE = "FhirPushTarget";

    // Valeurs d'auth acceptées.
    private static final List<String> AUTH_TYPES = List.of("NONE", "BASIC", "TOKEN");

    FhirPushTargetServiceImpl() {
        super(FhirPushTarget.class);
    }

    @Override
    protected FhirPushTargetDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FhirPushTarget> getTargets() {
        return baseObjectDAO.getAllOrdered("createdAt", true);
    }

    @Override
    @Transactional
    public FhirPushTarget createTarget(FhirPushTarget target) {
        // Toujours actif à la création (la désactivation est une action explicite) :
        // évite qu'un body injecte isActive="N" et crée une cible muette sans signal.
        target.setIsActive("Y");
        applyAuthFields(target, target);
        target.setAllowedResources(normalizeCsv(target.getAllowedResources()));
        target.setCreatedAt(Timestamp.from(Instant.now()));
        target.setSysUserId("1");
        String id = baseObjectDAO.insert(target);
        target.setId(id);
        syncToDataExport();
        return target;
    }

    @Override
    @Transactional
    public FhirPushTarget updateTarget(String id, FhirPushTarget changes) {
        FhirPushTarget target = baseObjectDAO.get(id).orElse(null);
        if (target == null) {
            return null;
        }
        String previousEndpoint = target.getEndpoint();
        // Refuse en amont un endpoint déjà pris par une AUTRE cible (sinon la
        // contrainte
        // d'unicité lèverait une exception brute à l'update).
        if (changes.getEndpoint() != null && !changes.getEndpoint().equals(previousEndpoint)) {
            FhirPushTarget clash = baseObjectDAO.findByEndpoint(changes.getEndpoint());
            if (clash != null && !clash.getId().equals(id)) {
                throw new IllegalArgumentException(
                        "endpoint déjà utilisé par une autre cible : " + changes.getEndpoint());
            }
        }
        target.setName(changes.getName());
        target.setDescription(changes.getDescription());
        target.setEndpoint(changes.getEndpoint());
        target.setAllowedResources(normalizeCsv(changes.getAllowedResources()));
        target.setIntervalMinutes(changes.getIntervalMinutes());
        applyAuthFields(target, changes);
        target.setSysUserId("1");
        baseObjectDAO.update(target);
        // Si l'endpoint a changé, l'ancienne tâche d'export doit être retirée.
        if (previousEndpoint != null && !previousEndpoint.equals(target.getEndpoint())) {
            removeDataExportTask(previousEndpoint);
        }
        syncToDataExport();
        return target;
    }

    @Override
    @Transactional
    public void setTargetActive(String id, boolean active) {
        FhirPushTarget target = baseObjectDAO.get(id).orElse(null);
        if (target == null) {
            return;
        }
        target.setIsActive(active ? "Y" : "N");
        target.setSysUserId("1");
        baseObjectDAO.update(target);
        syncToDataExport();
    }

    @Override
    @Transactional
    public void deleteTarget(String id) {
        FhirPushTarget target = baseObjectDAO.get(id).orElse(null);
        if (target == null) {
            return;
        }
        String endpoint = target.getEndpoint();
        baseObjectDAO.delete(target);
        removeDataExportTask(endpoint);
    }

    @Override
    @Transactional
    public void syncToDataExport() {
        for (FhirPushTarget target : baseObjectDAO.getAll()) {
            if ("Y".equals(target.getIsActive())) {
                upsertDataExportTask(target);
            } else {
                removeDataExportTask(target.getEndpoint());
            }
        }
    }

    /**
     * Crée/met à jour le DataExportTask (par endpoint) reflétant la cible active.
     * On ne prend en charge que les tâches gérées par cet écran (marqueur
     * {@link #MANAGED_HEADER}) : un DataExportTask pré-existant sur le même
     * endpoint mais SANS ce marqueur (ex. tâche native créée par
     * RegisterFhirHooksTask depuis {@code fhir.subscriber}) n'est PAS écrasé — on
     * refuse alors la projection pour éviter de dégrader/supprimer la sauvegarde
     * native.
     */
    private void upsertDataExportTask(FhirPushTarget target) {
        try {
            DataExportTask existing = dataExportTaskService.getDAO().findByEndpoint(target.getEndpoint()).orElse(null);
            if (existing != null && !isManaged(existing)) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "upsertDataExportTask",
                        "DataExportTask existant NON géré par l'écran sur l'endpoint " + target.getEndpoint()
                                + " : projection ignorée (ne pas écraser une tâche native).");
                return;
            }
            DataExportTask task = existing != null ? existing : new DataExportTask();
            task.setEndpoint(target.getEndpoint());
            task.setFhirResources(resolveResourceTypes(target));
            task.setMaxDataExportInterval(
                    target.getIntervalMinutes() != null && target.getIntervalMinutes() > 0 ? target.getIntervalMinutes()
                            : DEFAULT_INTERVAL_MINUTES);
            task.setDataRequestAttemptTimeout(DEFAULT_TIMEOUT_SECONDS);
            task.setHeaders(buildHeaders(target));
            dataExportTaskService.getDAO().save(task);
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "upsertDataExportTask", e.toString());
        }
    }

    private void removeDataExportTask(String endpoint) {
        try {
            dataExportTaskService.getDAO().findByEndpoint(endpoint).ifPresent(task -> {
                // Ne supprime QUE les tâches gérées par l'écran : ne jamais retirer une
                // tâche native (fhir.subscriber) qui partagerait le même endpoint.
                if (isManaged(task)) {
                    dataExportTaskService.getDAO().delete(task);
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "removeDataExportTask",
                            "DataExportTask non géré sur l'endpoint " + endpoint + " : suppression ignorée.");
                }
            });
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "removeDataExportTask", e.toString());
        }
    }

    /**
     * Une tâche est "gérée" par l'écran si elle porte le marqueur d'appartenance.
     */
    private boolean isManaged(DataExportTask task) {
        return task.getHeaders() != null && MANAGED_VALUE.equals(task.getHeaders().get(MANAGED_HEADER));
    }

    private List<ResourceType> resolveResourceTypes(FhirPushTarget target) {
        List<String> names = new ArrayList<>();
        if (!GenericValidator.isBlankOrNull(target.getAllowedResources())) {
            for (String r : target.getAllowedResources().split(",")) {
                if (!r.trim().isEmpty()) {
                    names.add(r.trim());
                }
            }
        }
        if (names.isEmpty()) {
            names = DEFAULT_RESOURCES;
        }
        List<ResourceType> types = new ArrayList<>();
        for (String name : names) {
            try {
                types.add(ResourceType.fromCode(name));
            } catch (RuntimeException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "resolveResourceTypes",
                        "type FHIR inconnu ignoré : " + name);
            }
        }
        // Si tous les types étaient invalides (ex. faute de frappe), on retombe sur la
        // liste par défaut plutôt que de projeter une tâche qui ne pousse rien.
        if (types.isEmpty()) {
            for (String name : DEFAULT_RESOURCES) {
                types.add(ResourceType.fromCode(name));
            }
        }
        return types;
    }

    /**
     * Headers ajoutés à chaque requête vers le distant : identité du site (comme le
     * flux natif) + Authorization selon le type d'auth de la cible.
     */
    private Map<String, String> buildHeaders(FhirPushTarget target) {
        Map<String, String> headers = new HashMap<>();
        headers.put(MANAGED_HEADER, MANAGED_VALUE);
        headers.put("Server-Name", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        headers.put("Server-Code", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));
        String authType = target.getAuthType();
        if ("BASIC".equalsIgnoreCase(authType) && !GenericValidator.isBlankOrNull(target.getAuthUsername())) {
            String creds = target.getAuthUsername() + ":"
                    + (target.getAuthSecret() == null ? "" : target.getAuthSecret());
            headers.put("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8)));
        } else if ("TOKEN".equalsIgnoreCase(authType) && !GenericValidator.isBlankOrNull(target.getAuthSecret())) {
            headers.put("Authorization", "Bearer " + target.getAuthSecret());
        }
        return headers;
    }

    /**
     * Applique et normalise les champs d'auth de {@code source} sur {@code target}
     * : type validé (valeur inconnue -> NONE) ; secret remplacé seulement s'il est
     * fourni (l'UI ne renvoie jamais le secret en lecture -> vide = inchangé) ; et
     * les champs sans objet sont vidés (NONE -> ni utilisateur ni secret ; TOKEN ->
     * pas d'utilisateur) pour ne pas laisser de credentials dormants.
     */
    private void applyAuthFields(FhirPushTarget target, FhirPushTarget source) {
        String authType = source.getAuthType();
        if (authType == null || !AUTH_TYPES.contains(authType.toUpperCase())) {
            authType = "NONE";
        } else {
            authType = authType.toUpperCase();
        }
        target.setAuthType(authType);
        if (!GenericValidator.isBlankOrNull(source.getAuthSecret())) {
            target.setAuthSecret(source.getAuthSecret());
        }
        target.setAuthUsername(source.getAuthUsername());
        if ("NONE".equals(authType)) {
            target.setAuthUsername(null);
            target.setAuthSecret(null);
        } else if ("TOKEN".equals(authType)) {
            target.setAuthUsername(null);
        }
    }

    private String normalizeCsv(String csv) {
        if (GenericValidator.isBlankOrNull(csv)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : csv.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(t);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
