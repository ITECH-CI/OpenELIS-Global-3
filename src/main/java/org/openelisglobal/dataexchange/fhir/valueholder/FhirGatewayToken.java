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
 * Jeton d'accès d'un tiers au mini-HIE (gateway FHIR). Rattaché à un
 * {@link FhirGatewayClient} (un client peut avoir plusieurs jetons : rotation).
 * Le jeton n'est JAMAIS stocké en clair : seul son hash SHA-256 est conservé
 * (tokenHash). Révocable à chaud via isActive. lastUsedAt trace le dernier
 * accès.
 */
public class FhirGatewayToken extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    // Client (tiers) propriétaire du jeton.
    private String clientId;

    // Hash SHA-256 (hex) du jeton présenté. Jamais le jeton en clair.
    private String tokenHash;

    // Révocation à chaud du jeton : "Y"/"N".
    private String isActive = "Y";

    private Timestamp createdAt;

    // Dernier accès autorisé avec ce jeton (audit, best-effort).
    private Timestamp lastUsedAt;

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

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
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

    public Timestamp getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Timestamp lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return "token:" + clientId;
    }
}
