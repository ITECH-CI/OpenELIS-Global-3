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
import java.util.Base64;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.dataexchange.fhir.dao.FhirGatewayTokenDAO;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirGatewayToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FhirGatewayTokenServiceImpl extends BaseObjectServiceImpl<FhirGatewayToken, String>
        implements FhirGatewayTokenService {

    @Autowired
    protected FhirGatewayTokenDAO baseObjectDAO;

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
            // Audit best-effort : trace le dernier accès. Une erreur ici ne doit pas
            // refuser un jeton pourtant valide.
            try {
                token.setLastUsedAt(Timestamp.from(Instant.now()));
                token.setSysUserId("1");
                baseObjectDAO.update(token);
            } catch (RuntimeException ignored) {
                // best-effort, on n'échoue pas la validation pour ça.
            }
            return true;
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "validateAndTouch", e.toString());
            return false;
        }
    }

    @Override
    @Transactional
    public String createToken(String clientName) {
        // Jeton opaque aléatoire (32 octets -> base64 url-safe). Communiqué EN CLAIR
        // une seule fois au client ; seul le hash est persisté.
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        FhirGatewayToken token = new FhirGatewayToken();
        token.setClientName(clientName);
        token.setTokenHash(sha256Hex(rawToken));
        token.setIsActive("Y");
        token.setCreatedAt(Timestamp.from(Instant.now()));
        token.setSysUserId("1");
        baseObjectDAO.insert(token);
        return rawToken;
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
