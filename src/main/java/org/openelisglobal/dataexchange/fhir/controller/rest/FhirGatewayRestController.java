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
package org.openelisglobal.dataexchange.fhir.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.dataexchange.fhir.service.FhirGatewayTokenService;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayAccessLog;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayClient;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway du mini-HIE : validation des jetons (pour nginx) + administration des
 * tiers (clients) et de leurs jetons.
 *
 * <p>
 * Seul {@code GET /rest/fhir-gateway/auth} est ouvert (pas de session) car
 * appelé par nginx via {@code auth_request} : renvoie 200 si le jeton (header
 * {@code Authorization: Bearer <t>} ou {@code X-API-Key}) est valide et actif,
 * 401 sinon. Il n'expose aucune donnée. Toutes les autres routes (gestion)
 * restent protégées par la session OE (admin).
 */
@RestController
@RequestMapping("/rest/fhir-gateway")
public class FhirGatewayRestController {

    @Autowired
    private FhirGatewayTokenService fhirGatewayTokenService;

    // Dates formatées côté API (jj/mm/aaaa hh:mm), comme le monitoring FHIR, pour
    // éviter d'exposer un timestamp brut à l'UI.
    private static final java.text.SimpleDateFormat DISPLAY_FORMAT = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");

    private static synchronized String formatDate(java.util.Date date) {
        return date != null ? DISPLAY_FORMAT.format(date) : null;
    }

    /**
     * Point de validation pour nginx auth_request (Phase C). Applique la décision
     * d'accès complète : jeton valide + méthode lecture seule + ressource autorisée
     * pour le tiers + quota non dépassé, et journalise l'accès. Renvoie 200
     * (autorisé), 401 (jeton invalide), 403 (méthode/ressource interdite) ou 429
     * (quota dépassé). nginx transmet la méthode et l'URI d'origine via les headers
     * {@code X-Original-Method} et {@code X-Original-URI}.
     */
    @GetMapping("/auth")
    public ResponseEntity<Void> auth(HttpServletRequest request) {
        String token = extractToken(request);
        String method = request.getHeader("X-Original-Method");
        String uri = request.getHeader("X-Original-URI");
        int status = fhirGatewayTokenService.authorizeAccess(token, method, uri);
        // Le module nginx auth_request ne propage proprement que 200/401/403 : tout
        // autre code (429...) devient un 500 côté client. On renvoie donc 403 à nginx
        // pour un refus de quota, tout en journalisant la vraie raison (429) en base.
        int nginxStatus = (status == 200 || status == 401) ? status : 403;
        return ResponseEntity.status(nginxStatus).build();
    }

    // ------------------------------------------------------------------
    // Administration (session OE requise)
    // ------------------------------------------------------------------

    /** Liste des tiers déclarés (sans les jetons). */
    @GetMapping("/clients")
    public List<Map<String, Object>> listClients() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FhirGatewayClient c : fhirGatewayTokenService.getClients()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("name", c.getName());
            row.put("description", c.getDescription());
            row.put("active", "Y".equals(c.getIsActive()));
            row.put("allowedResources", c.getAllowedResources());
            row.put("rateLimitPerMin", c.getRateLimitPerMin());
            out.add(row);
        }
        return out;
    }

    /** Crée un tiers. */
    @PostMapping("/clients")
    public ResponseEntity<Map<String, Object>> createClient(@RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        FhirGatewayClient client = fhirGatewayTokenService.createClient(name.trim(),
                description != null ? description.trim() : null);
        Map<String, Object> row = new HashMap<>();
        row.put("id", client.getId());
        row.put("name", client.getName());
        return ResponseEntity.ok(row);
    }

    /** Active/désactive un tiers (désactiver bloque tous ses jetons). */
    @PostMapping("/clients/{clientId}/active")
    public ResponseEntity<Void> setClientActive(@PathVariable String clientId, @RequestParam("active") boolean active) {
        fhirGatewayTokenService.setClientActive(clientId, active);
        return ResponseEntity.ok().build();
    }

    /**
     * Met à jour la config de durcissement (Phase C) d'un tiers : ressources FHIR
     * autorisées (CSV, vide = toutes) et quota de requêtes/minute (0 = illimité).
     */
    @PostMapping("/clients/{clientId}/policy")
    public ResponseEntity<Void> updateClientPolicy(@PathVariable String clientId,
            @RequestParam(value = "allowedResources", required = false) String allowedResources,
            @RequestParam(value = "rateLimitPerMin", required = false) Integer rateLimitPerMin) {
        fhirGatewayTokenService.updateClientPolicy(clientId, allowedResources, rateLimitPerMin);
        return ResponseEntity.ok().build();
    }

    /**
     * Journal d'audit des accès tiers (Phase C). Sans {@code clientId} : tous
     * clients confondus. {@code max} borne le nombre de lignes (défaut 100).
     */
    @GetMapping("/access-log")
    public List<Map<String, Object>> listAccessLog(@RequestParam(value = "clientId", required = false) String clientId,
            @RequestParam(value = "max", required = false, defaultValue = "100") int max) {
        int bounded = Math.max(1, Math.min(max, 500));
        List<FhirGatewayAccessLog> logs = (clientId == null || clientId.trim().isEmpty())
                ? fhirGatewayTokenService.getRecentAccessLogs(bounded)
                : fhirGatewayTokenService.getRecentAccessLogsForClient(clientId, bounded);
        List<Map<String, Object>> out = new ArrayList<>();
        for (FhirGatewayAccessLog l : logs) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", l.getId());
            row.put("clientId", l.getClientId());
            row.put("accessedAt", formatDate(l.getAccessedAt()));
            row.put("method", l.getMethod());
            row.put("resourceType", l.getResourceType());
            row.put("status", l.getStatus());
            out.add(row);
        }
        return out;
    }

    /**
     * Jetons d'un client (valeur en clair JAMAIS renvoyée ; seulement les
     * métadonnées).
     */
    @GetMapping("/clients/{clientId}/tokens")
    public List<Map<String, Object>> listTokens(@PathVariable String clientId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FhirGatewayToken t : fhirGatewayTokenService.getTokensForClient(clientId)) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", t.getId());
            row.put("active", "Y".equals(t.getIsActive()));
            row.put("createdAt", formatDate(t.getCreatedAt()));
            row.put("lastUsedAt", formatDate(t.getLastUsedAt()));
            out.add(row);
        }
        return out;
    }

    /** Émet un jeton pour un client ; renvoie la valeur EN CLAIR une seule fois. */
    @PostMapping("/clients/{clientId}/tokens")
    public ResponseEntity<Map<String, String>> issueToken(@PathVariable String clientId) {
        String rawToken = fhirGatewayTokenService.issueTokenForClient(clientId);
        Map<String, String> body = new HashMap<>();
        body.put("token", rawToken);
        body.put("note", "Conservez ce jeton : il ne sera plus jamais affiché (seul son hash est stocké).");
        return ResponseEntity.ok(body);
    }

    /** Révoque un jeton (par son id). */
    @PostMapping("/tokens/{tokenId}/revoke")
    public ResponseEntity<Void> revokeToken(@PathVariable String tokenId) {
        fhirGatewayTokenService.revokeToken(tokenId);
        return ResponseEntity.ok().build();
    }

    // Jeton depuis "Authorization: Bearer <t>" (prioritaire) ou "X-API-Key: <t>".
    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }
        return null;
    }
}
