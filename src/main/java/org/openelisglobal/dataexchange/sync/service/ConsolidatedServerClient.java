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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.sync.dto.DataSyncPayload;
import org.openelisglobal.dataexchange.sync.dto.VlRequestAckDTO;
import org.openelisglobal.dataexchange.sync.dto.VlRequestEventDTO;
import org.openelisglobal.dataexchange.sync.dto.VlRequestPullResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Transport de la remontée consolidée vers le serveur consolidé (SC), incrément
 * 4b. Encapsule l'authentification JWT (Basic → Bearer) et le POST du payload
 * JSON vers {@code {sync.apiUrl}/syncOrders}, avec un unique retry sur 401
 * (comme l'uploader). ACK = statut HTTP 2xx.
 *
 * <p>
 * Réutilise le bean partagé {@link CloseableHttpClient} d'OE (Apache
 * HttpClient). Le token JWT est obtenu à la demande (pas de rafraîchissement
 * proactif ; on ré-authentifie sur 401), stocké en mémoire {@code volatile}.
 */
@Component
public class ConsolidatedServerClient {

    @Autowired
    private CloseableHttpClient httpClient;

    /** Base URL du SC (POST {apiUrl}/syncOrders). */
    @Value("${org.openelisglobal.consolidated.sync.apiUrl:}")
    private String apiUrl;

    /** Endpoint d'obtention du JWT (Basic → token). */
    @Value("${org.openelisglobal.consolidated.sync.auth.url:}")
    private String authUrl;

    /** Identifiant Basic pour l'obtention du JWT. */
    @Value("${org.openelisglobal.consolidated.sync.username:}")
    private String username;

    /** Secret Basic pour l'obtention du JWT. */
    @Value("${org.openelisglobal.consolidated.sync.password:}")
    private String password;

    /**
     * Autorise le HTTP CLAIR vers le serveur consolidé (dev/local uniquement). Par
     * défaut false : les URL http:// sont réécrites en https:// et le payload
     * (données patient/résultats) + les jetons ne partent jamais en clair.
     */
    @Value("${org.openelisglobal.consolidated.sync.allowHTTP:false}")
    private boolean allowHttp;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private volatile String jwtToken;

    /**
     * Force HTTPS sur les URL du serveur consolidé après injection (sauf
     * allowHTTP=true). Même garde-fou que {@code RegisterFhirHooksTask} : sans TLS,
     * le Basic (user:pass) et le Bearer JWT transiteraient en clair.
     */
    @PostConstruct
    void normalizeUrls() {
        apiUrl = enforceHttps(apiUrl);
        authUrl = enforceHttps(authUrl);
    }

    private String enforceHttps(String url) {
        if (GenericValidator.isBlankOrNull(url)) {
            return url;
        }
        if (allowHttp && url.startsWith("http://")) {
            return url; // HTTP clair explicitement autorisé (dev/local).
        }
        if (url.startsWith("http://")) {
            return "https://" + url.substring("http://".length());
        }
        if (!url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }

    public boolean isConfigured() {
        return !GenericValidator.isBlankOrNull(apiUrl) && !GenericValidator.isBlankOrNull(authUrl);
    }

    /**
     * POST le payload vers {apiUrl}/syncOrders. Authentifie d'abord si nécessaire,
     * rejoue une fois sur 401 après ré-authentification.
     *
     * @return true si le SC a répondu 2xx (ACK)
     * @throws IOException erreur réseau/HTTP non récupérable
     */
    public boolean sendPayload(DataSyncPayload payload) throws IOException {
        String body = objectMapper.writeValueAsString(payload);
        if (jwtToken == null) {
            authenticate();
        }
        int status = post(body);
        if (status == 401) {
            // Token expiré/invalide : ré-authentifier et rejouer UNE fois.
            authenticate();
            status = post(body);
        }
        boolean ok = status >= 200 && status < 300;
        if (!ok) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "sendPayload",
                    "Le serveur consolidé a répondu HTTP " + status);
        }
        return ok;
    }

    private int post(String body) throws IOException {
        HttpPost httpPost = new HttpPost(join(apiUrl, "syncOrders"));
        httpPost.setHeader("Accept", "application/json");
        if (jwtToken != null) {
            httpPost.setHeader("Authorization", "Bearer " + jwtToken);
        }
        httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int code = response.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(response.getEntity());
            return code;
        }
    }

    /** Basic Auth → JWT ; stocke le token (accessToken de la réponse). */
    private void authenticate() throws IOException {
        if (GenericValidator.isBlankOrNull(authUrl)) {
            throw new IOException("URL d'authentification du serveur consolidé non configurée");
        }
        HttpPost authPost = new HttpPost(authUrl);
        String creds = (username == null ? "" : username) + ":" + (password == null ? "" : password);
        authPost.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8)));
        authPost.setHeader("Accept", "application/json");
        try (CloseableHttpResponse response = httpClient.execute(authPost)) {
            int code = response.getStatusLine().getStatusCode();
            String payload = response.getEntity() == null ? null
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (code < 200 || code >= 300 || payload == null) {
                throw new IOException("Échec d'authentification auprès du serveur consolidé (HTTP " + code + ")");
            }
            JwtResponse jwt = objectMapper.readValue(payload, JwtResponse.class);
            if (jwt == null || GenericValidator.isBlankOrNull(jwt.accessToken)) {
                throw new IOException("Réponse d'authentification sans accessToken");
            }
            this.jwtToken = jwt.accessToken;
        }
    }

    // === Pull entrant des demandes VL (incrément 4c) ===

    /**
     * GET {apiUrl}/v1/vl-requests?platformUuid=&limit=&since= — une page de
     * demandes. Ré-authentifie et rejoue une fois sur 401.
     *
     * @param platformUuid identifiant du labo (uuid côté SC) — requis
     * @param since        checkpoint opaque (vide/null au premier appel)
     * @param limit        taille de page
     * @return la page désérialisée, ou null en cas d'échec non récupérable
     */
    public VlRequestPullResponse pullRequests(String platformUuid, String since, int limit) throws IOException {
        StringBuilder url = new StringBuilder(join(apiUrl, "v1/vl-requests"));
        url.append("?platformUuid=").append(enc(platformUuid));
        url.append("&limit=").append(limit);
        if (!GenericValidator.isBlankOrNull(since)) {
            url.append("&since=").append(enc(since));
        }
        if (jwtToken == null) {
            authenticate();
        }
        HttpResult res = get(url.toString());
        if (res.status == 401) {
            authenticate();
            res = get(url.toString());
        }
        if (res.status < 200 || res.status >= 300) {
            throw new IOException("pull vl-requests a répondu HTTP " + res.status);
        }
        if (GenericValidator.isBlankOrNull(res.body)) {
            return null;
        }
        return objectMapper.readValue(res.body, VlRequestPullResponse.class);
    }

    /**
     * POST {apiUrl}/v1/vl-requests/ack — accuse l'import d'une demande. C'est l'ACK
     * "OK" qui retire la demande du flux de pull côté SC.
     *
     * @return true si le SC a répondu 2xx
     */
    public boolean ackRequest(VlRequestAckDTO ack) throws IOException {
        String body = objectMapper.writeValueAsString(ack);
        if (jwtToken == null) {
            authenticate();
        }
        int status = postTo(join(apiUrl, "v1/vl-requests/ack"), body);
        if (status == 401) {
            authenticate();
            status = postTo(join(apiUrl, "v1/vl-requests/ack"), body);
        }
        return status >= 200 && status < 300;
    }

    // === Push de statuts/résultats (incrément 4d) ===

    /**
     * POST {apiUrl}/v1/vl-requests/events — pousse un événement de statut/résultat.
     * Ré-authentifie et rejoue une fois sur 401.
     *
     * @return true si le SC a répondu 2xx.
     */
    public boolean pushEvent(VlRequestEventDTO event) throws IOException {
        String body = objectMapper.writeValueAsString(event);
        if (jwtToken == null) {
            authenticate();
        }
        int status = postTo(join(apiUrl, "v1/vl-requests/events"), body);
        if (status == 401) {
            authenticate();
            status = postTo(join(apiUrl, "v1/vl-requests/events"), body);
        }
        return status >= 200 && status < 300;
    }

    private HttpResult get(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        if (jwtToken != null) {
            httpGet.setHeader("Authorization", "Bearer " + jwtToken);
        }
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int code = response.getStatusLine().getStatusCode();
            String body = response.getEntity() == null ? null
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return new HttpResult(code, body);
        }
    }

    /** POST JSON vers une URL absolue (générique, réutilisé par ackRequest). */
    private int postTo(String url, String body) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Accept", "application/json");
        if (jwtToken != null) {
            httpPost.setHeader("Authorization", "Bearer " + jwtToken);
        }
        httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int code = response.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(response.getEntity());
            return code;
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String join(String base, String segment) {
        if (base.endsWith("/")) {
            return base + segment;
        }
        return base + "/" + segment;
    }

    /** Résultat HTTP minimal (code + corps). */
    private static final class HttpResult {
        final int status;
        final String body;

        HttpResult(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    /**
     * Réponse minimale de l'endpoint d'authentification (champs inconnus ignorés).
     */
    public static class JwtResponse {
        public String accessToken;
        public Integer expiresIn;
    }
}
