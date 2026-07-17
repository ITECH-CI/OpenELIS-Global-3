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
package org.openelisglobal.dataexchange.sync.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Source UNIQUE et DYNAMIQUE de la configuration du module d'échange (incrément
 * 5). Lit les réglages du serveur consolidé depuis {@code SiteInformation}
 * (clé/valeur admin, éditable sans redémarrage — les tasks appellent ces
 * getters à chaque tick). Le mot de passe est déchiffré automatiquement par
 * {@link SiteInformationService#getSiteInformationByName} (colonne
 * {@code encrypted=true}).
 *
 * <p>
 * <b>Rétro-compatibilité</b> : si une clé SiteInformation est vide/absente, on
 * retombe sur l'ancien {@code @Value("${org.openelisglobal.consolidated...}")}
 * (application.properties). Les déploiements existants continuent donc de
 * fonctionner tant que l'admin n'a rien saisi.
 *
 * <p>
 * <b>Limite assumée</b> : l'intervalle des {@code @Scheduled(fixedDelayString)}
 * est figé au démarrage de Spring et n'est PAS piloté ici (changer l'intervalle
 * nécessite un redémarrage). Seuls enabled/URLs/credentials/labUuid sont
 * dynamiques.
 */
@Service
public class SyncConfigService {

    // Noms de clés SiteInformation (≤ 32 caractères, cf. migration
    // add_consolidated_sync_config.xml).
    public static final String KEY_SYNC_ENABLED = "consolidatedSyncEnabled";
    public static final String KEY_PULL_ENABLED = "consolidatedPullEnabled";
    public static final String KEY_PUSH_ENABLED = "consolidatedPushEnabled";
    public static final String KEY_API_URL = "consolidatedSyncApiUrl";
    public static final String KEY_AUTH_URL = "consolidatedSyncAuthUrl";
    public static final String KEY_USERNAME = "consolidatedSyncUsername";
    public static final String KEY_PASSWORD = "consolidatedSyncPassword";
    public static final String KEY_LAB_UUID = "consolidatedSyncLabUuid";
    public static final String KEY_ALLOW_HTTP = "consolidatedSyncAllowHttp";

    public static final String DOMAIN = "consolidatedSync";

    @Autowired
    private SiteInformationService siteInformationService;

    // Fallbacks (anciens flags application.properties) — rétro-compat.
    @Value("${org.openelisglobal.consolidated.sync.enabled:false}")
    private boolean fallbackSyncEnabled;

    @Value("${org.openelisglobal.consolidated.pull.enabled:false}")
    private boolean fallbackPullEnabled;

    @Value("${org.openelisglobal.consolidated.push.enabled:false}")
    private boolean fallbackPushEnabled;

    @Value("${org.openelisglobal.consolidated.sync.apiUrl:}")
    private String fallbackApiUrl;

    @Value("${org.openelisglobal.consolidated.sync.auth.url:}")
    private String fallbackAuthUrl;

    @Value("${org.openelisglobal.consolidated.sync.username:}")
    private String fallbackUsername;

    @Value("${org.openelisglobal.consolidated.sync.password:}")
    private String fallbackPassword;

    @Value("${org.openelisglobal.consolidated.sync.labUuid:}")
    private String fallbackLabUuid;

    @Value("${org.openelisglobal.consolidated.sync.allowHTTP:false}")
    private boolean fallbackAllowHttp;

    public boolean isSyncEnabled() {
        return boolValue(KEY_SYNC_ENABLED, fallbackSyncEnabled);
    }

    public boolean isPullEnabled() {
        return boolValue(KEY_PULL_ENABLED, fallbackPullEnabled);
    }

    public boolean isPushEnabled() {
        return boolValue(KEY_PUSH_ENABLED, fallbackPushEnabled);
    }

    public String getApiUrl() {
        return textValue(KEY_API_URL, fallbackApiUrl);
    }

    public String getAuthUrl() {
        return textValue(KEY_AUTH_URL, fallbackAuthUrl);
    }

    public String getUsername() {
        return textValue(KEY_USERNAME, fallbackUsername);
    }

    /**
     * Mot de passe déchiffré (getSiteInformationByName déchiffre encrypted=true).
     */
    public String getPassword() {
        return textValue(KEY_PASSWORD, fallbackPassword);
    }

    public String getLabUuid() {
        return textValue(KEY_LAB_UUID, fallbackLabUuid);
    }

    public boolean isAllowHttp() {
        return boolValue(KEY_ALLOW_HTTP, fallbackAllowHttp);
    }

    /** Serveur consolidé configuré = apiUrl ET authUrl non vides. */
    public boolean isConfigured() {
        return !GenericValidator.isBlankOrNull(getApiUrl()) && !GenericValidator.isBlankOrNull(getAuthUrl());
    }

    // --- Lecture SiteInformation avec fallback -----------------------------------

    private String textValue(String key, String fallback) {
        String db = readSiteInfo(key);
        return GenericValidator.isBlankOrNull(db) ? fallback : db;
    }

    private boolean boolValue(String key, boolean fallback) {
        String db = readSiteInfo(key);
        if (GenericValidator.isBlankOrNull(db)) {
            return fallback;
        }
        return "true".equalsIgnoreCase(db.trim());
    }

    /**
     * Cache TTL court par clé : évite le N+1 SELECT (les tasks appellent ces
     * getters plusieurs fois par tick — ex. par demande dans une page). La config
     * change rarement ; {@value #CACHE_TTL_MS} ms de fraîcheur est imperceptible et
     * garde la dynamicité (un changement admin est vu au tick suivant + TTL).
     */
    private static final long CACHE_TTL_MS = 5000L;

    private final Map<String, CachedValue> cache = new ConcurrentHashMap<>();

    private String readSiteInfo(String key) {
        long now = System.currentTimeMillis();
        CachedValue cached = cache.get(key);
        if (cached != null && now - cached.timestamp < CACHE_TTL_MS) {
            return cached.value;
        }
        try {
            SiteInformation info = siteInformationService.getSiteInformationByName(key);
            String value = info == null ? null : info.getValue();
            cache.put(key, new CachedValue(value, now));
            return value;
        } catch (RuntimeException e) {
            // Lecture non bloquante : en cas d'erreur, on retombe sur le fallback.
            LogEvent.logWarn(this.getClass().getSimpleName(), "readSiteInfo",
                    "lecture config '" + key + "' échouée, fallback properties : " + e.getMessage());
            return null;
        }
    }

    private static final class CachedValue {
        final String value;
        final long timestamp;

        CachedValue(String value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
