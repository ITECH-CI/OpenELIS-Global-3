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
 * Journal d'audit d'un accès tiers au mini-HIE (Phase C). Une ligne par requête
 * évaluée par l'endpoint /auth : quel client, quand, quelle méthode/ressource,
 * et le statut de la décision (200 autorisé, 401 jeton invalide, 403 ressource
 * non autorisée, 429 quota dépassé).
 */
public class FhirGatewayAccessLog extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    // Client identifié (null si le jeton est inconnu/invalide).
    private String clientId;

    private Timestamp accessedAt;

    // Méthode HTTP d'origine (transmise par nginx).
    private String method;

    // Type de ressource FHIR demandé (première composante du chemin, ex.
    // "Patient").
    private String resourceType;

    private String requestUri;

    // Statut HTTP renvoyé à nginx pour cette requête.
    private Integer status;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Timestamp getAccessedAt() {
        return accessedAt;
    }

    public void setAccessedAt(Timestamp accessedAt) {
        this.accessedAt = accessedAt;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return resourceType;
    }
}
