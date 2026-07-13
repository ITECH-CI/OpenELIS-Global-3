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

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayToken;

/** Gestion des jetons d'accès des tiers au mini-HIE (gateway FHIR). */
public interface FhirGatewayTokenService extends BaseObjectService<FhirGatewayToken, String> {

    /**
     * Valide un jeton présenté en clair : hache, cherche un jeton ACTIF
     * correspondant, met à jour last_used_at (best-effort). Renvoie true si le
     * jeton est valide et actif. Ne lève jamais : renvoie false sur jeton vide ou
     * erreur.
     */
    boolean validateAndTouch(String rawToken);

    /**
     * Crée un nouveau jeton pour un tiers et renvoie sa valeur EN CLAIR (à
     * communiquer une seule fois au client ; seul le hash est stocké).
     */
    String createToken(String clientName);
}
