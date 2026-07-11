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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.dataexchange.terminology.controller.rest;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.dataexchange.terminology.service.TerminologyImportReport;
import org.openelisglobal.dataexchange.terminology.service.TerminologyImportService;
import org.openelisglobal.dataexchange.terminology.service.TerminologyTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API d'import terminologique (LOINC / SNOMED CT) depuis CSV. Deux étapes : /preview
 * (dry-run, n'écrit rien) puis /apply (persiste). Le CSV est reçu en corps de
 * requête brut (text/plain ou text/csv). Voir README terminology pour le format.
 */
@RestController
@RequestMapping("/rest/terminology-import")
public class TerminologyImportRestController {

    @Autowired
    private TerminologyImportService terminologyImportService;

    /** Liste des cibles disponibles (pour peupler un select côté UI). */
    @GetMapping("/targets")
    public List<String> targets() {
        List<String> targets = new ArrayList<>();
        for (TerminologyTarget target : TerminologyTarget.values()) {
            targets.add(target.name());
        }
        return targets;
    }

    /** Dry-run : analyse le CSV et renvoie le rapport sans rien écrire en base. */
    @PostMapping(value = "/preview", consumes = { MediaType.TEXT_PLAIN_VALUE, "text/csv" })
    public ResponseEntity<TerminologyImportReport> preview(@RequestParam("target") String target,
            @RequestBody String csvContent) {
        TerminologyTarget parsed = TerminologyTarget.fromString(target);
        if (parsed == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(terminologyImportService.preview(parsed, csvContent));
    }

    /** Application : met à jour les entités et renvoie le rapport. Idempotent. */
    @PostMapping(value = "/apply", consumes = { MediaType.TEXT_PLAIN_VALUE, "text/csv" })
    public ResponseEntity<TerminologyImportReport> apply(@RequestParam("target") String target,
            @RequestBody String csvContent) {
        TerminologyTarget parsed = TerminologyTarget.fromString(target);
        if (parsed == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(terminologyImportService.apply(parsed, csvContent));
    }
}
