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
package org.openelisglobal.dataexchange.fhir.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Suivi d'un ÉVÉNEMENT de transformation/persistance FHIR (une ligne par appel
 * de transformation : entrée de commande, saisie de résultats, validation,
 * patient…). Permet de rendre visibles les échecs (aujourd'hui silencieux
 * car @Async) et de les rejouer (retry ciblé + job programmé).
 */
public class FhirSyncStatus extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    public enum SyncStatus {
        PENDING, // en attente / en cours
        SUCCESS, // transformation + persistance FHIR OK
        FAILED // échec (voir errorMessage) — rejouable
    }

    private String id;

    // Déclencheur de la transformation, ex. ORDER_ENTRY, RESULTS, VALIDATION,
    // PATIENT, ORGANIZATION. Texte libre côté producteur (constantes côté service).
    private String triggerType;

    // Cible métier de la transformation (ce qu'on rejoue) : type + id OE.
    // Ex. targetType=SAMPLE, targetId=<sampleId> ; ou PATIENT, <patientId>.
    private String targetType;
    private String targetId;

    private String status = SyncStatus.PENDING.name();

    private Integer attemptCount = 0;

    private Timestamp lastAttemptAt;

    // Message d'erreur (tronqué) du dernier échec, pour le monitoring.
    private String errorMessage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Timestamp getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(Timestamp lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return triggerType + ":" + targetType + ":" + targetId;
    }
}
