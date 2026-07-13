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
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.dataexchange.fhir.service.FhirGatewayTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway du mini-HIE : validation des jetons d'accès des tiers.
 *
 * <p>
 * {@code GET /rest/fhir-gateway/auth} est appelé par nginx via {@code
 * auth_request} avant de proxifier {@code /fhir/} vers le HAPI : renvoie 200 si
 * le jeton (header {@code Authorization: Bearer <t>} ou {@code X-API-Key}) est
 * valide et actif, 401 sinon. Endpoint volontairement léger (pas de corps).
 *
 * <p>
 * Seul {@code /rest/fhir-gateway/auth} est ouvert (pas de session) car appelé
 * par nginx : voir SecurityConfig OPEN_PAGES. Sa seule fonction est de valider
 * un jeton — il n'expose aucune donnée. La création de jeton ({@code /token})
 * reste protégée par la session OE.
 */
@RestController
@RequestMapping("/rest/fhir-gateway")
public class FhirGatewayRestController {

    @Autowired
    private FhirGatewayTokenService fhirGatewayTokenService;

    /**
     * Point de validation pour nginx auth_request. 200 = autorisé, 401 = refusé.
     */
    @GetMapping("/auth")
    public ResponseEntity<Void> auth(HttpServletRequest request) {
        String token = extractToken(request);
        if (fhirGatewayTokenService.validateAndTouch(token)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Crée un jeton pour un tiers et renvoie sa valeur EN CLAIR (à communiquer une
     * seule fois). Réservé aux administrateurs (sous /rest/**, authentifié par la
     * session OE).
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> createToken(@RequestParam("clientName") String clientName) {
        if (clientName == null || clientName.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String rawToken = fhirGatewayTokenService.createToken(clientName.trim());
        Map<String, String> body = new HashMap<>();
        body.put("clientName", clientName.trim());
        body.put("token", rawToken);
        body.put("note", "Conservez ce jeton : il ne sera plus jamais affiché (seul son hash est stocké).");
        return ResponseEntity.ok(body);
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
