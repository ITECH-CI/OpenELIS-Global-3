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

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayAccessLog;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayClient;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayToken;

/**
 * Gateway du mini-HIE : gestion des tiers (clients) et de leurs jetons d'accès
 * FHIR. Un client peut détenir plusieurs jetons (rotation). Le service porte à
 * la fois la validation (chemin nginx auth_request) et l'administration.
 */
public interface FhirGatewayTokenService extends BaseObjectService<FhirGatewayToken, String> {

    /**
     * Valide un jeton présenté en clair : hache, cherche un jeton ACTIF dont le
     * client est ACTIF, met à jour last_used_at (best-effort). Renvoie true si
     * valide. Ne lève jamais : false sur jeton vide ou erreur.
     */
    boolean validateAndTouch(String rawToken);

    /**
     * Décision d'accès complète (Phase C, durcissement) pour nginx auth_request :
     * valide le jeton, applique les règles administrables du tiers (lecture seule,
     * ressources autorisées, quota/min) et journalise l'accès. Renvoie le statut
     * HTTP à retourner à nginx : 200 (autorisé), 401 (jeton invalide), 403 (méthode
     * ou ressource interdite), 429 (quota dépassé).
     *
     * @param rawToken   jeton en clair présenté par le tiers
     * @param method     méthode HTTP d'origine (transmise par nginx), ex. "GET"
     * @param requestUri URI d'origine, ex. "/fhir/Patient?..."
     */
    int authorizeAccess(String rawToken, String method, String requestUri);

    // --- Administration des clients ---

    /** Liste des tiers déclarés (plus récents en tête). */
    List<FhirGatewayClient> getClients();

    /** Crée un tiers. Renvoie l'entité créée. */
    FhirGatewayClient createClient(String name, String description);

    /** Active/désactive un tiers (désactiver bloque tous ses jetons). */
    void setClientActive(String clientId, boolean active);

    /**
     * Met à jour la config de durcissement d'un tiers (Phase C) :
     * {@code allowedResources} (CSV de types FHIR, null/vide = tous) et
     * {@code rateLimitPerMin} (null/0 = illimité).
     */
    void updateClientPolicy(String clientId, String allowedResources, Integer rateLimitPerMin);

    // --- Audit (Phase C) ---

    /** Derniers accès tiers (tous clients), du plus récent au plus ancien. */
    List<FhirGatewayAccessLog> getRecentAccessLogs(int max);

    /** Derniers accès d'un client donné. */
    List<FhirGatewayAccessLog> getRecentAccessLogsForClient(String clientId, int max);

    // --- Administration des jetons ---

    /** Jetons d'un client (hash masqué côté UI ; on ne renvoie jamais le clair). */
    List<FhirGatewayToken> getTokensForClient(String clientId);

    /**
     * Émet un nouveau jeton pour un client et renvoie sa valeur EN CLAIR (à
     * communiquer une seule fois ; seul le hash est stocké).
     */
    String issueTokenForClient(String clientId);

    /** Révoque (désactive) un jeton par son id. */
    void revokeToken(String tokenId);
}
