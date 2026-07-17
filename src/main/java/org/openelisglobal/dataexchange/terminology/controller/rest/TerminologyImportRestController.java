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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API d'import terminologique (LOINC / SNOMED CT) depuis CSV. Deux étapes :
 * /preview (dry-run, n'écrit rien) puis /apply (persiste). La requête est un
 * JSON {@code { target, overwrite, csv }} (application/json, comme les autres
 * POST du projet — évite les soucis de converter/CSRF sur text/plain). Voir le
 * README terminology pour le format du CSV lui-même.
 */
@RestController
@RequestMapping("/rest/terminology-import")
public class TerminologyImportRestController {

    @Autowired
    private TerminologyImportService terminologyImportService;

    /** Corps JSON de la requête d'import (preview/apply). */
    public static class ImportRequest {
        private String target;
        private boolean overwrite;
        private String csv;

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public boolean isOverwrite() {
            return overwrite;
        }

        public void setOverwrite(boolean overwrite) {
            this.overwrite = overwrite;
        }

        public String getCsv() {
            return csv;
        }

        public void setCsv(String csv) {
            this.csv = csv;
        }
    }

    /** Liste des cibles disponibles (pour peupler un select côté UI). */
    @GetMapping("/targets")
    public List<String> targets() {
        List<String> targets = new ArrayList<>();
        for (TerminologyTarget target : TerminologyTarget.values()) {
            targets.add(target.name());
        }
        return targets;
    }

    /**
     * Dry-run : analyse le CSV et renvoie le rapport sans rien écrire en base.
     * overwrite=true → les codes différents d'un existant sortent en WOULD_UPDATE ;
     * false (défaut) → en CONFLICT.
     */
    @PostMapping(value = "/preview")
    public ResponseEntity<TerminologyImportReport> preview(@RequestBody ImportRequest request) {
        TerminologyTarget parsed = TerminologyTarget.fromString(request.getTarget());
        if (parsed == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(terminologyImportService.preview(parsed, request.getCsv(), request.isOverwrite()));
    }

    /**
     * Application : met à jour les entités et renvoie le rapport. Idempotent.
     * overwrite=true → écrase les codes existants différents ; false (défaut) → les
     * laisse intacts et les signale en CONFLICT.
     */
    @PostMapping(value = "/apply")
    public ResponseEntity<TerminologyImportReport> apply(@RequestBody ImportRequest request) {
        TerminologyTarget parsed = TerminologyTarget.fromString(request.getTarget());
        if (parsed == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(terminologyImportService.apply(parsed, request.getCsv(), request.isOverwrite()));
    }
}
