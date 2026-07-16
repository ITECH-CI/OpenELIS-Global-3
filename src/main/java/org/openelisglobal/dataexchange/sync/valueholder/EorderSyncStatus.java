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
package org.openelisglobal.dataexchange.sync.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Suivi du push des statuts/résultats d'une demande CV importée vers le serveur
 * consolidé (module d'échange unifié, incrément 4d). Alimentée par les triggers
 * CDC (voir {@code eorder_status_cdc_triggers.sql}) via le lien natif
 * {@code sample.clinical_order_id → electronic_order} ; drainée par
 * {@code DataPushTask}.
 *
 * <p>
 * {@code syncFlag} (cf. {@link EorderSyncFlag}) — mémorise aussi le dernier
 * statut/résultat envoyé ({@code lastSentLabStatus}/{@code lastSentResult})
 * pour l'anti-régression et l'anti-boucle.
 */
public class EorderSyncStatus extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    /** Valeurs de {@code sync_flag} (miroir de la sémantique uploader). */
    public static final class EorderSyncFlag {
        public static final short PENDING = 0;
        public static final short IMPORTED = 1;
        public static final short TO_UPDATE = 2;
        public static final short SYNCED = 3;
        public static final short IN_PROGRESS = 4;
        public static final short SEND_FAILED = 6;

        private EorderSyncFlag() {
        }
    }

    private String id;
    private String requestUuid;
    private String labno;
    private String sampleId;
    private String analysisId;
    private String electronicOrderId;
    private String labStatus;
    private String lastSentLabStatus;
    private String lastSentResult;
    private String lastEventUuid;
    private Short syncFlag;
    private Timestamp lastSyncedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getRequestUuid() {
        return requestUuid;
    }

    public void setRequestUuid(String requestUuid) {
        this.requestUuid = requestUuid;
    }

    public String getLabno() {
        return labno;
    }

    public void setLabno(String labno) {
        this.labno = labno;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getElectronicOrderId() {
        return electronicOrderId;
    }

    public void setElectronicOrderId(String electronicOrderId) {
        this.electronicOrderId = electronicOrderId;
    }

    public String getLabStatus() {
        return labStatus;
    }

    public void setLabStatus(String labStatus) {
        this.labStatus = labStatus;
    }

    public String getLastSentLabStatus() {
        return lastSentLabStatus;
    }

    public void setLastSentLabStatus(String lastSentLabStatus) {
        this.lastSentLabStatus = lastSentLabStatus;
    }

    public String getLastSentResult() {
        return lastSentResult;
    }

    public void setLastSentResult(String lastSentResult) {
        this.lastSentResult = lastSentResult;
    }

    public String getLastEventUuid() {
        return lastEventUuid;
    }

    public void setLastEventUuid(String lastEventUuid) {
        this.lastEventUuid = lastEventUuid;
    }

    public Short getSyncFlag() {
        return syncFlag;
    }

    public void setSyncFlag(Short syncFlag) {
        this.syncFlag = syncFlag;
    }

    public Timestamp getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Timestamp lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
