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
package org.openelisglobal.dataexchange.fhir.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Base64;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.dataexchange.fhir.dao.FhirGatewayAccessLogDAO;
import org.openelisglobal.dataexchange.fhir.dao.FhirGatewayClientDAO;
import org.openelisglobal.dataexchange.fhir.dao.FhirGatewayTokenDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayAccessLog;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayClient;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FhirGatewayTokenServiceImpl extends BaseObjectServiceImpl<FhirGatewayToken, String>
        implements FhirGatewayTokenService {

    @Autowired
    protected FhirGatewayTokenDAO baseObjectDAO;

    @Autowired
    private FhirGatewayClientDAO clientDAO;

    @Autowired
    private FhirGatewayAccessLogDAO accessLogDAO;

    // Fenêtre glissante d'une minute par client (timestamps epoch-ms des requêtes
    // autorisées récentes). En mémoire : le quota est un garde-fou best-effort, il
    // se réinitialise au redémarrage et n'est pas partagé entre instances.
    private final ConcurrentHashMap<String, Deque<Long>> rateWindows = new ConcurrentHashMap<>();

    private static final long ONE_MINUTE_MS = 60_000L;

    FhirGatewayTokenServiceImpl() {
        super(FhirGatewayToken.class);
    }

    @Override
    protected FhirGatewayTokenDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public boolean validateAndTouch(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return false;
        }
        try {
            String hash = sha256Hex(rawToken.trim());
            FhirGatewayToken token = baseObjectDAO.findActiveByTokenHash(hash);
            if (token == null) {
                return false;
            }
            // Le client propriétaire doit être actif : désactiver un client révoque
            // tous ses jetons.
            FhirGatewayClient client = clientDAO.get(token.getClientId()).orElse(null);
            if (client == null || !"Y".equals(client.getIsActive())) {
                return false;
            }
            // Audit best-effort : une erreur ici ne doit pas refuser un jeton valide.
            try {
                token.setLastUsedAt(Timestamp.from(Instant.now()));
                token.setSysUserId("1");
                baseObjectDAO.update(token);
            } catch (RuntimeException ignored) {
                // best-effort
            }
            return true;
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "validateAndTouch", e.toString());
            return false;
        }
    }

    @Override
    @Transactional
    public int authorizeAccess(String rawToken, String method, String requestUri) {
        String resourceType = extractResourceType(requestUri);
        FhirGatewayClient client = null;
        int status;
        try {
            // Un seul hash + une seule recherche du jeton pour tout le chemin de
            // décision (hot path : appelé à chaque requête FHIR).
            FhirGatewayToken token = (rawToken == null || rawToken.trim().isEmpty()) ? null
                    : baseObjectDAO.findActiveByTokenHash(sha256Hex(rawToken.trim()));
            client = token == null ? null : clientDAO.get(token.getClientId()).orElse(null);
            if (client == null || !"Y".equals(client.getIsActive())) {
                client = null;
                status = 401;
            } else if (!isReadMethod(method)) {
                // Le mini-HIE en lecture n'autorise que GET/HEAD ; toute écriture est
                // refusée (la réception d'ordres passe par /fhir-in, pas par /fhir).
                status = 403;
            } else if (!isResourceAllowed(client, resourceType)) {
                status = 403;
            } else if (isRateLimited(client)) {
                status = 429;
            } else {
                touchLastUsed(token);
                status = 200;
            }
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "authorizeAccess", e.toString());
            status = 401;
        }
        writeAccessLog(client, method, resourceType, requestUri, status);
        return status;
    }

    private boolean isReadMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || method == null;
    }

    /**
     * Vrai si le type de ressource est autorisé pour ce client. Liste vide/nulle =
     * tout autorisé. Comparaison insensible à la casse. Les requêtes hors ressource
     * (métadonnées : "metadata", "", "_history"...) restent autorisées.
     */
    private boolean isResourceAllowed(FhirGatewayClient client, String resourceType) {
        String allowed = client.getAllowedResources();
        if (GenericValidator.isBlankOrNull(allowed)) {
            return true;
        }
        if (GenericValidator.isBlankOrNull(resourceType) || resourceType.startsWith("_")
                || "metadata".equalsIgnoreCase(resourceType)) {
            return true;
        }
        Set<String> set = new HashSet<>();
        for (String r : allowed.split(",")) {
            String t = r.trim().toLowerCase();
            if (!t.isEmpty()) {
                set.add(t);
            }
        }
        return set.contains(resourceType.toLowerCase());
    }

    /**
     * Applique le quota/minute du client (fenêtre glissante en mémoire). Renvoie
     * true si la limite est DÉPASSÉE (accès à refuser). 0/null = illimité. Chaque
     * appel non-limité enregistre l'horodatage courant.
     */
    private synchronized boolean isRateLimited(FhirGatewayClient client) {
        Integer limit = client.getRateLimitPerMin();
        if (limit == null || limit <= 0) {
            return false;
        }
        long now = Instant.now().toEpochMilli();
        Deque<Long> window = rateWindows.computeIfAbsent(client.getId(), k -> new ArrayDeque<>());
        while (!window.isEmpty() && now - window.peekFirst() >= ONE_MINUTE_MS) {
            window.pollFirst();
        }
        if (window.size() >= limit) {
            return true;
        }
        window.addLast(now);
        return false;
    }

    private void touchLastUsed(FhirGatewayToken token) {
        try {
            if (token != null) {
                token.setLastUsedAt(Timestamp.from(Instant.now()));
                token.setSysUserId("1");
                baseObjectDAO.update(token);
            }
        } catch (RuntimeException ignored) {
            // best-effort
        }
    }

    private void writeAccessLog(FhirGatewayClient client, String method, String resourceType, String requestUri,
            int status) {
        try {
            FhirGatewayAccessLog log = new FhirGatewayAccessLog();
            log.setClientId(client != null ? client.getId() : null);
            log.setAccessedAt(Timestamp.from(Instant.now()));
            log.setMethod(method);
            log.setResourceType(resourceType);
            log.setRequestUri(truncate(requestUri, 1000));
            log.setStatus(status);
            log.setSysUserId("1");
            accessLogDAO.insert(log);
        } catch (RuntimeException e) {
            // L'audit ne doit jamais bloquer la décision d'accès.
            LogEvent.logWarn(this.getClass().getSimpleName(), "writeAccessLog", e.toString());
        }
    }

    /**
     * Extrait le type de ressource FHIR d'une URI, ex. "/fhir/Patient?..." ->
     * "Patient". Renvoie null si indéterminable.
     */
    private String extractResourceType(String requestUri) {
        if (GenericValidator.isBlankOrNull(requestUri)) {
            return null;
        }
        String path = requestUri;
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        // Retire un éventuel préfixe "/fhir" puis découpe.
        String[] parts = Arrays.stream(path.split("/")).filter(s -> !s.isEmpty() && !"fhir".equalsIgnoreCase(s))
                .toArray(String[]::new);
        return parts.length == 0 ? null : parts[0];
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    // --- Clients ---

    @Override
    @Transactional(readOnly = true)
    public List<FhirGatewayClient> getClients() {
        return clientDAO.getAllOrdered("createdAt", true);
    }

    @Override
    @Transactional
    public FhirGatewayClient createClient(String name, String description) {
        FhirGatewayClient client = new FhirGatewayClient();
        client.setName(name);
        client.setDescription(description);
        client.setIsActive("Y");
        client.setCreatedAt(Timestamp.from(Instant.now()));
        client.setSysUserId("1");
        String id = clientDAO.insert(client);
        client.setId(id);
        return client;
    }

    @Override
    @Transactional
    public void setClientActive(String clientId, boolean active) {
        FhirGatewayClient client = clientDAO.get(clientId).orElse(null);
        if (client == null) {
            return;
        }
        client.setIsActive(active ? "Y" : "N");
        client.setSysUserId("1");
        clientDAO.update(client);
    }

    @Override
    @Transactional
    public void updateClientPolicy(String clientId, String allowedResources, Integer rateLimitPerMin) {
        FhirGatewayClient client = clientDAO.get(clientId).orElse(null);
        if (client == null) {
            return;
        }
        // Normalise la liste CSV (trim, retire les entrées vides) ; null/vide = tous.
        client.setAllowedResources(normalizeCsv(allowedResources));
        client.setRateLimitPerMin(rateLimitPerMin != null && rateLimitPerMin > 0 ? rateLimitPerMin : null);
        client.setSysUserId("1");
        clientDAO.update(client);
    }

    private String normalizeCsv(String csv) {
        if (GenericValidator.isBlankOrNull(csv)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : csv.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(t);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    // --- Audit ---

    @Override
    @Transactional(readOnly = true)
    public List<FhirGatewayAccessLog> getRecentAccessLogs(int max) {
        return accessLogDAO.getRecent(max);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FhirGatewayAccessLog> getRecentAccessLogsForClient(String clientId, int max) {
        return accessLogDAO.getRecentForClient(clientId, max);
    }

    // --- Jetons ---

    @Override
    @Transactional(readOnly = true)
    public List<FhirGatewayToken> getTokensForClient(String clientId) {
        return baseObjectDAO.getByClientId(clientId);
    }

    @Override
    @Transactional
    public String issueTokenForClient(String clientId) {
        // Jeton opaque aléatoire (32 octets -> base64 url-safe). Communiqué EN CLAIR
        // une seule fois ; seul le hash est persisté.
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        FhirGatewayToken token = new FhirGatewayToken();
        token.setClientId(clientId);
        token.setTokenHash(sha256Hex(rawToken));
        token.setIsActive("Y");
        token.setCreatedAt(Timestamp.from(Instant.now()));
        token.setSysUserId("1");
        baseObjectDAO.insert(token);
        return rawToken;
    }

    @Override
    @Transactional
    public void revokeToken(String tokenId) {
        FhirGatewayToken token = baseObjectDAO.get(tokenId).orElse(null);
        if (token == null) {
            return;
        }
        token.setIsActive("N");
        token.setSysUserId("1");
        baseObjectDAO.update(token);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 indisponible", e);
        }
    }
}
