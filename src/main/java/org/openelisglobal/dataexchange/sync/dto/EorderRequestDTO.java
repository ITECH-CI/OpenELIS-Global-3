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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Demande de charge virale servie par le serveur consolidé au pull entrant
 * (incrément 4c), miroir (partiel) de son entité
 * {@code VlElectronicRequestFlat} sérialisée en JSON plat camelCase.
 *
 * <p>
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} : le SC expose ~90 champs
 * (dont beaucoup de mécanique interne : fhir*, retry*, record*, ingestedAt…).
 * On ne déclare QUE les champs utiles à l'import côté OE ; les autres sont
 * ignorés. L'objet complet est de toute façon conservé tel quel dans
 * {@code electronic_order.data} (JSON).
 *
 * <p>
 * {@code requestUuid} est la clé métier stable à conserver et à renvoyer dans
 * l'ACK. Elle sert d'{@code external_id} de l'ElectronicOrder (idempotence).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EorderRequestDTO {

    // Identité / corrélation
    private String requestUuid;
    private String serviceRequestId;
    private String placerId;
    private String taskIdentifier;
    private String taskId;

    // Patient (anonymisé côté SC : pas de nom/prénom)
    private String patientCode;
    private String patientSubjectNumber;
    private String gender;
    private LocalDate birthDate;
    private Integer ageYear;
    private Integer ageMonth;

    // Site demandeur
    private String requestingSiteCode;
    private String requestingSiteName;
    private String requestingDistrict;
    private String requestingRegion;
    private String implementingPartner;

    // Demande
    private LocalDateTime authoredOn;
    private LocalDate requestDate;
    private String orderReason;
    private String otherOrderReason;
    private String nameOfSampler;
    private String nameOfRequestor;

    // Spécimen
    private String sampleType;
    private LocalDateTime collectionDate;
    private LocalDateTime receivedDate;
    private String labno;

    // Plateforme
    private String requestedPlatformName;
    private String destinationPlatformName;

    public String getRequestUuid() {
        return requestUuid;
    }

    public void setRequestUuid(String requestUuid) {
        this.requestUuid = requestUuid;
    }

    public String getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(String serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public String getPlacerId() {
        return placerId;
    }

    public void setPlacerId(String placerId) {
        this.placerId = placerId;
    }

    public String getTaskIdentifier() {
        return taskIdentifier;
    }

    public void setTaskIdentifier(String taskIdentifier) {
        this.taskIdentifier = taskIdentifier;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getPatientSubjectNumber() {
        return patientSubjectNumber;
    }

    public void setPatientSubjectNumber(String patientSubjectNumber) {
        this.patientSubjectNumber = patientSubjectNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getAgeYear() {
        return ageYear;
    }

    public void setAgeYear(Integer ageYear) {
        this.ageYear = ageYear;
    }

    public Integer getAgeMonth() {
        return ageMonth;
    }

    public void setAgeMonth(Integer ageMonth) {
        this.ageMonth = ageMonth;
    }

    public String getRequestingSiteCode() {
        return requestingSiteCode;
    }

    public void setRequestingSiteCode(String requestingSiteCode) {
        this.requestingSiteCode = requestingSiteCode;
    }

    public String getRequestingSiteName() {
        return requestingSiteName;
    }

    public void setRequestingSiteName(String requestingSiteName) {
        this.requestingSiteName = requestingSiteName;
    }

    public String getRequestingDistrict() {
        return requestingDistrict;
    }

    public void setRequestingDistrict(String requestingDistrict) {
        this.requestingDistrict = requestingDistrict;
    }

    public String getRequestingRegion() {
        return requestingRegion;
    }

    public void setRequestingRegion(String requestingRegion) {
        this.requestingRegion = requestingRegion;
    }

    public String getImplementingPartner() {
        return implementingPartner;
    }

    public void setImplementingPartner(String implementingPartner) {
        this.implementingPartner = implementingPartner;
    }

    public LocalDateTime getAuthoredOn() {
        return authoredOn;
    }

    public void setAuthoredOn(LocalDateTime authoredOn) {
        this.authoredOn = authoredOn;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public String getOrderReason() {
        return orderReason;
    }

    public void setOrderReason(String orderReason) {
        this.orderReason = orderReason;
    }

    public String getOtherOrderReason() {
        return otherOrderReason;
    }

    public void setOtherOrderReason(String otherOrderReason) {
        this.otherOrderReason = otherOrderReason;
    }

    public String getNameOfSampler() {
        return nameOfSampler;
    }

    public void setNameOfSampler(String nameOfSampler) {
        this.nameOfSampler = nameOfSampler;
    }

    public String getNameOfRequestor() {
        return nameOfRequestor;
    }

    public void setNameOfRequestor(String nameOfRequestor) {
        this.nameOfRequestor = nameOfRequestor;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public LocalDateTime getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDateTime collectionDate) {
        this.collectionDate = collectionDate;
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getLabno() {
        return labno;
    }

    public void setLabno(String labno) {
        this.labno = labno;
    }

    public String getRequestedPlatformName() {
        return requestedPlatformName;
    }

    public void setRequestedPlatformName(String requestedPlatformName) {
        this.requestedPlatformName = requestedPlatformName;
    }

    public String getDestinationPlatformName() {
        return destinationPlatformName;
    }

    public void setDestinationPlatformName(String destinationPlatformName) {
        this.destinationPlatformName = destinationPlatformName;
    }
}
