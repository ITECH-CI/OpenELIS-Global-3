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
package org.openelisglobal.dataexchange.sync.dto;

/**
 * Accusé d'import d'une demande VL (incrément 4c) :
 * {@code POST /api/v1/vl-requests/ack}. C'est l'ACK "OK" qui fait passer la
 * demande à {@code IMPORTED_IN_OPENELIS} côté SC et la RETIRE du flux de pull
 * (sinon re-livraison à chaque cycle).
 *
 * <p>
 * {@code eventUuid} est la clé d'idempotence côté SC : pour un même import, il
 * doit être STABLE (rejouer le même eventUuid n'a pas d'effet). On le dérive
 * donc de {@code requestUuid} (déterministe), pas d'un UUID aléatoire — ainsi
 * un retry d'ACK réutilise le même eventUuid.
 */
public class VlRequestAckDTO {

    private String eventUuid;
    private String requestUuid;
    private String platform;
    private String importStatus;
    private String labno;
    private Long sampleId;
    private Long analysisId;
    private String errorMessage;

    public String getEventUuid() {
        return eventUuid;
    }

    public void setEventUuid(String eventUuid) {
        this.eventUuid = eventUuid;
    }

    public String getRequestUuid() {
        return requestUuid;
    }

    public void setRequestUuid(String requestUuid) {
        this.requestUuid = requestUuid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(String importStatus) {
        this.importStatus = importStatus;
    }

    public String getLabno() {
        return labno;
    }

    public void setLabno(String labno) {
        this.labno = labno;
    }

    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
