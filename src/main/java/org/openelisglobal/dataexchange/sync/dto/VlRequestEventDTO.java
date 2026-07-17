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

import java.time.LocalDateTime;

/**
 * Événement de statut/résultat poussé au serveur consolidé (incrément 4d) :
 * {@code POST /api/v1/vl-requests/events}. Noms de champs alignés sur le
 * contrat SC (ci.itech.oedatarepo.dto.VlRequestEventDTO) — ne renseigner que
 * les champs non-null (le SC n'écrase l'existant que par les champs non-null ;
 * un null ne remet jamais à zéro).
 *
 * <p>
 * {@code eventUuid} = clé d'idempotence côté SC : STABLE pour un même
 * changement (rejeu = no-op), DIFFÉRENT pour un changement réel (sinon le
 * nouveau statut est ignoré). {@code labStatus} doit être un nom exact d'enum
 * {@code EorderLabStatus}
 * (ACCEPTED/IN_PROGRESS/COMPLETED/RELEASED/FAILED/NON_CONFORM/REJECTED/CANCELLED).
 */
public class VlRequestEventDTO {

    private String eventUuid;
    private String requestUuid;
    private String eventType;
    private String labStatus;
    private String labno;
    private String testResult;
    private Long testResultInt;
    private String resultUnit;
    private String resultComment;
    private LocalDateTime completedDate;
    private LocalDateTime releasedDate;
    private LocalDateTime receivedDate;
    private String errorMessage;
    private String rejectReason;
    private String rejectComment;
    private String rejectAuthor;

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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getLabStatus() {
        return labStatus;
    }

    public void setLabStatus(String labStatus) {
        this.labStatus = labStatus;
    }

    public String getLabno() {
        return labno;
    }

    public void setLabno(String labno) {
        this.labno = labno;
    }

    public String getTestResult() {
        return testResult;
    }

    public void setTestResult(String testResult) {
        this.testResult = testResult;
    }

    public Long getTestResultInt() {
        return testResultInt;
    }

    public void setTestResultInt(Long testResultInt) {
        this.testResultInt = testResultInt;
    }

    public String getResultUnit() {
        return resultUnit;
    }

    public void setResultUnit(String resultUnit) {
        this.resultUnit = resultUnit;
    }

    public String getResultComment() {
        return resultComment;
    }

    public void setResultComment(String resultComment) {
        this.resultComment = resultComment;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public LocalDateTime getReleasedDate() {
        return releasedDate;
    }

    public void setReleasedDate(LocalDateTime releasedDate) {
        this.releasedDate = releasedDate;
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getRejectComment() {
        return rejectComment;
    }

    public void setRejectComment(String rejectComment) {
        this.rejectComment = rejectComment;
    }

    public String getRejectAuthor() {
        return rejectAuthor;
    }

    public void setRejectAuthor(String rejectAuthor) {
        this.rejectAuthor = rejectAuthor;
    }
}
