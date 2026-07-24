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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.service.FhirPushTargetService;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirPushTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administration des cibles de <b>push FHIR distant</b> (mini-HIE, session OE
 * requise). CRUD des cibles + supervision (dernier essai / dernier succès du
 * push). L'état de supervision est lu directement sur la cible (colonnes
 * {@code last_attempt}/{@code last_success} tenues à jour par le moteur natif
 * {@code FhirPushEngineServiceImpl}) — plus aucune dépendance au module
 * {@code dataexport}.
 */
@RestController
@RequestMapping("/rest/fhir-push-targets")
public class FhirPushTargetRestController {

    @Autowired
    private FhirPushTargetService fhirPushTargetService;

    /**
     * Liste des cibles + leur état de push (dernier essai/succès). Secret jamais
     * renvoyé.
     */
    @GetMapping
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FhirPushTarget t : fhirPushTargetService.getTargets()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", t.getId());
            row.put("name", t.getName());
            row.put("description", t.getDescription());
            row.put("endpoint", t.getEndpoint());
            row.put("allowedResources", t.getAllowedResources());
            row.put("intervalMinutes", t.getIntervalMinutes());
            row.put("authType", t.getAuthType());
            row.put("authUsername", t.getAuthUsername());
            // Indique si un secret est défini, sans le divulguer.
            row.put("hasSecret", !GenericValidator.isBlankOrNull(t.getAuthSecret()));
            row.put("active", "Y".equals(t.getIsActive()));
            row.put("lastAttempt", isoOrNull(t.getLastAttempt()));
            row.put("lastSuccess", isoOrNull(t.getLastSuccess()));
            out.add(row);
        }
        return out;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody FhirPushTarget body) {
        if (body == null || GenericValidator.isBlankOrNull(body.getName())
                || GenericValidator.isBlankOrNull(body.getEndpoint())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            FhirPushTarget created = fhirPushTargetService.createTarget(body);
            Map<String, Object> row = new HashMap<>();
            row.put("id", created.getId());
            row.put("name", created.getName());
            return ResponseEntity.ok(row);
        } catch (RuntimeException e) {
            // Nom/endpoint déjà utilisé (contrainte d'unicité) ou données invalides.
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable String id, @RequestBody FhirPushTarget body) {
        if (body == null || GenericValidator.isBlankOrNull(body.getName())
                || GenericValidator.isBlankOrNull(body.getEndpoint())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            FhirPushTarget updated = fhirPushTargetService.updateTarget(id, body);
            return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            // Endpoint déjà pris par une autre cible, ou contrainte d'unicité.
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/active")
    public ResponseEntity<Void> setActive(@PathVariable String id, @RequestParam("active") boolean active) {
        fhirPushTargetService.setTargetActive(id, active);
        return ResponseEntity.ok().build();
    }

    // POST (et non DELETE) : le conteneur (web.xml security-constraint) n'autorise
    // que GET/POST/PUT sur l'application.
    @PostMapping("/{id}/delete")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        fhirPushTargetService.deleteTarget(id);
        return ResponseEntity.ok().build();
    }

    private static String isoOrNull(Timestamp ts) {
        return ts == null ? null : ts.toInstant().toString();
    }
}
