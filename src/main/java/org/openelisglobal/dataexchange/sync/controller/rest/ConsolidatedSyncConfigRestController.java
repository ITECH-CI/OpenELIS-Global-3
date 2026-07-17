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
package org.openelisglobal.dataexchange.sync.controller.rest;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.sync.service.SyncConfigService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Config admin des flux de sync vers le serveur consolidé (incrément 5).
 * Lit/écrit les clés {@code SiteInformation} du domaine
 * {@code consolidatedSync}. Écran admin dédié (protégé GLOBAL_ADMIN via
 * {@code /admin}).
 *
 * <p>
 * Le mot de passe n'est JAMAIS renvoyé (seul un booléen {@code hasPassword}
 * sort) ; à l'écriture, un mot de passe vide = « inchangé ». Les changements
 * sont pris en compte au prochain tick des tasks (qui relisent la config en
 * base via {@code SyncConfigService}, cache TTL ~5 s).
 */
@RestController
@RequestMapping("/rest/consolidated-sync-config")
public class ConsolidatedSyncConfigRestController {

    // Borne du password EN CLAIR : ~110 chars garantissent un ciphertext AES256
    // base64 sous la limite VARCHAR(200) de site_information.value.
    private static final int MAX_PASSWORD_LENGTH = 110;

    @Autowired
    private SiteInformationService siteInformationService;

    @GetMapping
    public Map<String, Object> get() {
        Map<String, Object> out = new HashMap<>();
        out.put("syncEnabled", boolVal(SyncConfigService.KEY_SYNC_ENABLED));
        out.put("pullEnabled", boolVal(SyncConfigService.KEY_PULL_ENABLED));
        out.put("pushEnabled", boolVal(SyncConfigService.KEY_PUSH_ENABLED));
        out.put("apiUrl", textVal(SyncConfigService.KEY_API_URL));
        out.put("authUrl", textVal(SyncConfigService.KEY_AUTH_URL));
        out.put("username", textVal(SyncConfigService.KEY_USERNAME));
        out.put("labUuid", textVal(SyncConfigService.KEY_LAB_UUID));
        out.put("allowHttp", boolVal(SyncConfigService.KEY_ALLOW_HTTP));
        // Le mot de passe n'est jamais divulgué : seulement l'indication qu'il existe.
        out.put("hasPassword", !GenericValidator.isBlankOrNull(textVal(SyncConfigService.KEY_PASSWORD)));
        return out;
    }

    /**
     * {@code @Transactional} : les 9 clés sont écrites tout-ou-rien. Sans cette
     * frontière, un échec au milieu (ex. valeur trop longue) laisserait la config
     * persistée à moitié (URLs activées mais password absent → auth cassée).
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> save(@RequestBody ConfigForm form) {
        if (form == null) {
            return ResponseEntity.badRequest().build();
        }
        // La colonne site_information.value est VARCHAR(200) ; le password chiffré
        // (AES256 base64) fait ~1.5-3x le clair. On borne le clair pour échouer
        // proprement (400) plutôt que par une erreur SQL au milieu de la transaction.
        if (!GenericValidator.isBlankOrNull(form.password) && form.password.length() > MAX_PASSWORD_LENGTH) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "password too long");
            return ResponseEntity.badRequest().body(err);
        }
        try {
            setValue(SyncConfigService.KEY_SYNC_ENABLED, Boolean.toString(form.syncEnabled));
            setValue(SyncConfigService.KEY_PULL_ENABLED, Boolean.toString(form.pullEnabled));
            setValue(SyncConfigService.KEY_PUSH_ENABLED, Boolean.toString(form.pushEnabled));
            setValue(SyncConfigService.KEY_ALLOW_HTTP, Boolean.toString(form.allowHttp));
            setValue(SyncConfigService.KEY_API_URL, nullToEmpty(form.apiUrl));
            setValue(SyncConfigService.KEY_AUTH_URL, nullToEmpty(form.authUrl));
            setValue(SyncConfigService.KEY_USERNAME, nullToEmpty(form.username));
            setValue(SyncConfigService.KEY_LAB_UUID, nullToEmpty(form.labUuid));
            // Mot de passe : n'écrase QUE s'il est fourni non-vide (vide = inchangé).
            if (!GenericValidator.isBlankOrNull(form.password)) {
                setValue(SyncConfigService.KEY_PASSWORD, form.password);
            }
            // NB : pas de loadDBValuesIntoConfiguration() ici — les tasks lisent la config
            // directement en base (SyncConfigService) au prochain tick, PAS le cache
            // ConfigurationProperties. L'appeler ne servirait qu'à recharger tout le cache
            // global (coûteux) sans effet sur ce module.

            Map<String, Object> ok = new HashMap<>();
            ok.put("saved", true);
            return ResponseEntity.ok(ok);
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "save", "échec sauvegarde config sync : " + e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "save failed");
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Écrit une clé via le SERVICE (update encrypte si la ligne est
     * encrypted=true). NE PAS utiliser updateSiteInformationByName : ce chemin
     * appelle le DAO directement et court-circuite le chiffrement (le mot de passe
     * serait stocké en clair).
     */
    private void setValue(String key, String value) {
        SiteInformation info = siteInformationService.getSiteInformationByName(key);
        if (info == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "setValue", "clé de config absente : " + key);
            return;
        }
        info.setValue(value);
        siteInformationService.update(info);
    }

    private String textVal(String key) {
        SiteInformation info = siteInformationService.getSiteInformationByName(key);
        return info == null ? "" : nullToEmpty(info.getValue());
    }

    private boolean boolVal(String key) {
        SiteInformation info = siteInformationService.getSiteInformationByName(key);
        return info != null && "true".equalsIgnoreCase(nullToEmpty(info.getValue()).trim());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Corps de la requête de sauvegarde. */
    public static class ConfigForm {
        public boolean syncEnabled;
        public boolean pullEnabled;
        public boolean pushEnabled;
        public boolean allowHttp;
        public String apiUrl;
        public String authUrl;
        public String username;
        public String password;
        public String labUuid;
    }
}
