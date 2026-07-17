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
 * Tiers autorisé à accéder au mini-HIE (SIGDEP, EMR, autre LIS). Un client peut
 * détenir plusieurs {@link FhirGatewayToken} (rotation sans perdre l'identité
 * du client). Désactiver le client ({@code isActive=N}) bloque tous ses jetons.
 */
public class FhirGatewayClient extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    // Nom lisible du tiers (ex. "SIGDEP-CV site X"). Unique.
    private String name;

    // Description / contact (optionnel).
    private String description;

    // Révocation globale du tiers : "Y"/"N".
    private String isActive = "Y";

    // Durcissement (Phase C) : types de ressources FHIR autorisés en lecture, en
    // liste CSV (ex. "Patient,ServiceRequest"). null/vide = tous autorisés.
    private String allowedResources;

    // Durcissement (Phase C) : quota de requêtes/minute pour ce tiers. null/0 =
    // illimité.
    private Integer rateLimitPerMin;

    private Timestamp createdAt;

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

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getAllowedResources() {
        return allowedResources;
    }

    public void setAllowedResources(String allowedResources) {
        this.allowedResources = allowedResources;
    }

    public Integer getRateLimitPerMin() {
        return rateLimitPerMin;
    }

    public void setRateLimitPerMin(Integer rateLimitPerMin) {
        this.rateLimitPerMin = rateLimitPerMin;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return name;
    }
}
