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
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.dataexchange.fhir.service.FhirOrderReceptionService;
import org.openelisglobal.dataexchange.fhir.service.FhirOrderReceptionService.ReceptionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réception PUSH d'ordres FHIR par des tiers (mini-HIE, Phase B). Un tiers POST
 * un Bundle {ServiceRequest, QuestionnaireResponse, Patient, Specimen} SANS
 * Task ; OE crée un ElectronicOrder "en attente" (repris via l'entrée
 * d'échantillon, où OE construit le Task).
 *
 * <p>
 * Cet endpoint est exposé via nginx {@code location /fhir-in/} qui applique la
 * même sécurité (jeton + IP allowlist) que la lecture {@code /fhir/}. Le
 * contrôle d'accès est donc au niveau nginx (auth_request) — cet endpoint est
 * listé dans SecurityConfig OPEN_PAGES.
 */
@RestController
@RequestMapping("/rest/fhir-in")
public class FhirInboundRestController {

    @Autowired
    private FhirOrderReceptionService fhirOrderReceptionService;

    /**
     * Réception d'un Bundle FHIR (JSON) contenant un ou des ServiceRequest. Le
     * corps est lu brut (pas via @RequestBody, qui ferait passer un JSON
     * application/json par Jackson et échouerait à le désérialiser en String) —
     * HAPI parse ensuite.
     */
    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> receiveOrder(HttpServletRequest request) {
        String bundleJson;
        try (BufferedReader reader = request.getReader()) {
            bundleJson = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("accepted", false);
            err.put("message", "lecture du corps impossible : " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
        ReceptionResult result = fhirOrderReceptionService.receiveOrderBundle(bundleJson);
        Map<String, Object> body = new HashMap<>();
        body.put("accepted", result.isAccepted());
        body.put("orderNumber", result.getOrderNumber());
        body.put("message", result.getMessage());
        // 201 si un ordre a été créé, 422 si le Bundle est valide mais non exploitable
        // (test/patient incomplet), 400 sinon (déjà couvert par le service via
        // message).
        HttpStatus status = result.isAccepted() ? HttpStatus.CREATED : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(body);
    }
}
