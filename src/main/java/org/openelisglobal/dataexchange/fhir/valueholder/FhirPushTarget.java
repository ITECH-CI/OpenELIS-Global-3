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
package org.openelisglobal.dataexchange.fhir.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Cible de <b>push FHIR distant</b> administrable depuis OE (mini-HIE, sens
 * sortant). Porte les paramètres éditables par l'admin (endpoint, ressources,
 * fréquence, auth, actif) ainsi que le point de reprise + la supervision du
 * moteur natif ({@code lastPushed}/{@code lastAttempt}/{@code lastSuccess}). Le
 * moteur {@code FhirPushEngineServiceImpl} lit cette table à chaque tick et
 * pousse le delta du store FHIR local vers chaque cible active ; désactiver une
 * cible ({@code isActive=N}) suffit à ce qu'il cesse de la pousser.
 */
public class FhirPushTarget extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    // Nom lisible de la cible (ex. "Serveur consolidé national"). Unique.
    private String name;

    private String description;

    // URL du serveur FHIR distant. Unique (clé de projection vers
    // data_export_task).
    private String endpoint;

    // Types FHIR poussés, liste CSV (vide/null = liste par défaut du service).
    private String allowedResources;

    // Fréquence d'export en minutes (null = valeur par défaut du service).
    private Integer intervalMinutes;

    // Auth vers le distant : "NONE" / "BASIC" / "TOKEN".
    private String authType = "NONE";

    private String authUsername;

    // Mot de passe (BASIC) ou jeton (TOKEN).
    private String authSecret;

    // Révocation de la cible : "Y"/"N". "N" met la cible en pause (le moteur natif
    // ne la pousse plus).
    private String isActive = "Y";

    private Timestamp createdAt;

    // Point de reprise du moteur de push FHIR natif : borne HAUTE du dernier delta
    // poussé avec succès (sert de borne BASSE exclusive de la prochaine fenêtre
    // _lastUpdated). null = jamais poussé (première fenêtre = tout l'historique).
    private Timestamp lastPushed;

    // Supervision : dernier tick ayant tenté un push (succès ou échec).
    private Timestamp lastAttempt;

    // Supervision : dernier push réussi (affiché dans l'écran admin).
    private Timestamp lastSuccess;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAllowedResources() {
        return allowedResources;
    }

    public void setAllowedResources(String allowedResources) {
        this.allowedResources = allowedResources;
    }

    public Integer getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(Integer intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getAuthSecret() {
        return authSecret;
    }

    public void setAuthSecret(String authSecret) {
        this.authSecret = authSecret;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getLastPushed() {
        return lastPushed;
    }

    public void setLastPushed(Timestamp lastPushed) {
        this.lastPushed = lastPushed;
    }

    public Timestamp getLastAttempt() {
        return lastAttempt;
    }

    public void setLastAttempt(Timestamp lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    public Timestamp getLastSuccess() {
        return lastSuccess;
    }

    public void setLastSuccess(Timestamp lastSuccess) {
        this.lastSuccess = lastSuccess;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return name;
    }
}
