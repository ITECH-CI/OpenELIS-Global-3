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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dataexchange.fhir.dao.FhirPushTargetDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirPushTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion administrable des cibles de push FHIR distant (mini-HIE). L'écran
 * admin ne fait qu'écrire cette table ; le {@code FhirPushEngineServiceImpl}
 * (moteur natif) la lit à chaque tick, pousse le delta du store FHIR local vers
 * chaque cible active, et met à jour le point de reprise + la supervision
 * (colonnes {@code last_pushed}/{@code last_attempt}/{@code last_success}).
 *
 * <p>
 * Ce service ne dépend plus du module {@code dataexport} (retiré) : la
 * projection dans {@code data_export_task} a disparu au profit d'une lecture
 * directe de la table par le moteur natif. Activer/désactiver/supprimer une
 * cible suffit — le moteur cesse (ou reprend) automatiquement de la pousser au
 * tick suivant, sans réconciliation d'une table tierce.
 */
@Service
public class FhirPushTargetServiceImpl extends AuditableBaseObjectServiceImpl<FhirPushTarget, String>
        implements FhirPushTargetService {

    @Autowired
    protected FhirPushTargetDAO baseObjectDAO;

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
    @Transactional(readOnly = true)
    public List<FhirPushTarget> getActiveTargets() {
        List<FhirPushTarget> active = new ArrayList<>();
        for (FhirPushTarget t : baseObjectDAO.getAll()) {
            if ("Y".equals(t.getIsActive())) {
                active.add(t);
            }
        }
        return active;
    }

    @Override
    @Transactional
    public void markPushAttempt(String id) {
        FhirPushTarget target = baseObjectDAO.get(id).orElse(null);
        if (target == null) {
            return;
        }
        target.setLastAttempt(Timestamp.from(Instant.now()));
        target.setSysUserId("1");
        baseObjectDAO.update(target);
    }

    @Override
    @Transactional
    public void markPushSuccess(String id, Timestamp checkpoint) {
        FhirPushTarget target = baseObjectDAO.get(id).orElse(null);
        if (target == null) {
            return;
        }
        // On n'AVANCE jamais le point de reprise en arrière : si un autre chemin l'a
        // déjà porté plus loin, on garde le max (robustesse face aux ticks
        // concurrents).
        if (checkpoint != null && (target.getLastPushed() == null || checkpoint.after(target.getLastPushed()))) {
            target.setLastPushed(checkpoint);
        }
        target.setLastSuccess(Timestamp.from(Instant.now()));
        target.setSysUserId("1");
        baseObjectDAO.update(target);
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
        // contrainte d'unicité lèverait une exception brute à l'update).
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
        // Changement d'endpoint = nouvelle destination : on réinitialise le point de
        // reprise pour que la nouvelle cible reçoive tout l'historique (sinon elle
        // repartirait du checkpoint de l'ancienne destination, qui n'a rien reçu).
        if (previousEndpoint != null && !previousEndpoint.equals(target.getEndpoint())) {
            target.setLastPushed(null);
        }
        target.setSysUserId("1");
        baseObjectDAO.update(target);
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
    }

    @Override
    @Transactional
    public void deleteTarget(String id) {
        FhirPushTarget target = baseObjectDAO.get(id).orElse(null);
        if (target == null) {
            return;
        }
        baseObjectDAO.delete(target);
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
