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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Demande (sample + patient + organisation) remontée au serveur consolidé. Noms
 * JSON alignés sur le contrat SC (ci.itech.oedatarepo.dto.OrderDTO).
 *
 * <p>
 * Les listes imbriquées {@code orderMetadata} et {@code analyses} du contrat
 * d'origine sont volontairement ABSENTES : le SC ne les lit jamais (il ne
 * consomme que les listes top-level {@code orderMetadataDTOs} et {@code
 * analysisDTOs} du payload). {@code sampleLocalId} est la clé d'upsert côté SC.
 * Cf. DESIGN_INTEROP_MODULE_CIV.md §6.2.
 */
public class OrderDTO {

    private Long sampleLocalId;
    private String labno;
    private LocalDateTime sampleEnteredDate;
    private LocalDateTime sampleReceivedDate;
    private LocalDateTime sampleCollectionDate;
    private String sampleStatus;
    private String sampleReferringId;
    private LocalDateTime sampleLastupdated;
    private String project;
    private String organizationDhis2Code;
    private String organizationName;
    private String organizationDatimCode;
    private String organizationDatimName;
    private String sampleProvider;
    private Long patientLocalId;
    private String patientNationalId;
    private String patientUpidCode;
    private String patientExternalId;
    private String patientGuid;
    private String patientGender;
    private LocalDate patientBirthdate;

    public Long getSampleLocalId() {
        return sampleLocalId;
    }

    public void setSampleLocalId(Long sampleLocalId) {
        this.sampleLocalId = sampleLocalId;
    }

    public String getLabno() {
        return labno;
    }

    public void setLabno(String labno) {
        this.labno = labno;
    }

    public LocalDateTime getSampleEnteredDate() {
        return sampleEnteredDate;
    }

    public void setSampleEnteredDate(LocalDateTime sampleEnteredDate) {
        this.sampleEnteredDate = sampleEnteredDate;
    }

    public LocalDateTime getSampleReceivedDate() {
        return sampleReceivedDate;
    }

    public void setSampleReceivedDate(LocalDateTime sampleReceivedDate) {
        this.sampleReceivedDate = sampleReceivedDate;
    }

    public LocalDateTime getSampleCollectionDate() {
        return sampleCollectionDate;
    }

    public void setSampleCollectionDate(LocalDateTime sampleCollectionDate) {
        this.sampleCollectionDate = sampleCollectionDate;
    }

    public String getSampleStatus() {
        return sampleStatus;
    }

    public void setSampleStatus(String sampleStatus) {
        this.sampleStatus = sampleStatus;
    }

    public String getSampleReferringId() {
        return sampleReferringId;
    }

    public void setSampleReferringId(String sampleReferringId) {
        this.sampleReferringId = sampleReferringId;
    }

    public LocalDateTime getSampleLastupdated() {
        return sampleLastupdated;
    }

    public void setSampleLastupdated(LocalDateTime sampleLastupdated) {
        this.sampleLastupdated = sampleLastupdated;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getOrganizationDhis2Code() {
        return organizationDhis2Code;
    }

    public void setOrganizationDhis2Code(String organizationDhis2Code) {
        this.organizationDhis2Code = organizationDhis2Code;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationDatimCode() {
        return organizationDatimCode;
    }

    public void setOrganizationDatimCode(String organizationDatimCode) {
        this.organizationDatimCode = organizationDatimCode;
    }

    public String getOrganizationDatimName() {
        return organizationDatimName;
    }

    public void setOrganizationDatimName(String organizationDatimName) {
        this.organizationDatimName = organizationDatimName;
    }

    public String getSampleProvider() {
        return sampleProvider;
    }

    public void setSampleProvider(String sampleProvider) {
        this.sampleProvider = sampleProvider;
    }

    public Long getPatientLocalId() {
        return patientLocalId;
    }

    public void setPatientLocalId(Long patientLocalId) {
        this.patientLocalId = patientLocalId;
    }

    public String getPatientNationalId() {
        return patientNationalId;
    }

    public void setPatientNationalId(String patientNationalId) {
        this.patientNationalId = patientNationalId;
    }

    public String getPatientUpidCode() {
        return patientUpidCode;
    }

    public void setPatientUpidCode(String patientUpidCode) {
        this.patientUpidCode = patientUpidCode;
    }

    public String getPatientExternalId() {
        return patientExternalId;
    }

    public void setPatientExternalId(String patientExternalId) {
        this.patientExternalId = patientExternalId;
    }

    public String getPatientGuid() {
        return patientGuid;
    }

    public void setPatientGuid(String patientGuid) {
        this.patientGuid = patientGuid;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public LocalDate getPatientBirthdate() {
        return patientBirthdate;
    }

    public void setPatientBirthdate(LocalDate patientBirthdate) {
        this.patientBirthdate = patientBirthdate;
    }
}
