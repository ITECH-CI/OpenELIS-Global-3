package org.openelisglobal.organization.controller.rest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.organization.service.OrganizationReferentielService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réconciliation manuelle des organisations avec le référentiel national des
 * établissements : recherche, aperçu, fusion, annulation. Réservé aux
 * administrateurs (URL non déclarée pour un rôle :
 * ModuleAuthenticationInterceptor n'y laisse passer que l'administrateur).
 */
@RestController
@RequestMapping("/rest/organization-referentiel")
public class OrganizationReferentielRestController {

    @Autowired
    private OrganizationReferentielService referentielService;

    @GetMapping("/recherche")
    public List<Map<String, Object>> rechercher(@RequestParam String terme,
            @RequestParam(required = false) String niveau) {
        return referentielService.rechercher(terme, niveau);
    }

    @GetMapping("/apercu")
    public ResponseEntity<?> apercu(@RequestParam String a, @RequestParam String b) {
        try {
            return ResponseEntity.ok(referentielService.apercu(a, b));
        } catch (RuntimeException e) {
            return erreur("apercu", e);
        }
    }

    /**
     * Corps : {@code {"a": "...", "b": "...", "survivante": "..." (facultatif),
     * "motif": "..."}}.
     */
    @PostMapping("/fusion")
    public ResponseEntity<?> fusionner(@RequestBody Map<String, String> demande) {
        try {
            return ResponseEntity.ok(referentielService.fusionner(demande.get("a"), demande.get("b"),
                    demande.get("survivante"), demande.get("motif")));
        } catch (RuntimeException e) {
            return erreur("fusionner", e);
        }
    }

    @PostMapping("/fusion/{fusionId}/annulation")
    public ResponseEntity<?> annuler(@PathVariable long fusionId) {
        try {
            return ResponseEntity.ok(referentielService.annuler(fusionId));
        } catch (RuntimeException e) {
            return erreur("annuler", e);
        }
    }

    @GetMapping("/fusions")
    public List<Map<String, Object>> fusions() {
        return referentielService.fusions();
    }

    /**
     * 400 avec le message utile : celui de la fonction SQL (RAISE EXCEPTION, ex.
     * deux codes d'établissement différents) plutôt que l'enveloppe JDBC.
     */
    private ResponseEntity<?> erreur(String methode, RuntimeException e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage() == null ? e.toString() : cause.getMessage();
        int i = message.indexOf("ERROR: ");
        if (i >= 0) {
            message = message.substring(i + 7);
        }
        int j = message.indexOf("\n");
        if (j > 0) {
            message = message.substring(0, j);
        }
        LogEvent.logWarn(this.getClass().getSimpleName(), methode, message);
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("erreur", message);
        return ResponseEntity.badRequest().body(corps);
    }
}
