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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.dataexchange.sync.dto.EorderRequestDTO;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EorderImportServiceImpl implements EorderImportService {

    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private PersonService personService;
    @Autowired
    private PatientIdentityService identityService;
    @Autowired
    private PatientIdentityTypeService identityTypeService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private IStatusService statusService;

    @Override
    @Transactional
    public ImportResult importRequest(EorderRequestDTO request, String rawJson) {
        if (request == null || GenericValidator.isBlankOrNull(request.getRequestUuid())) {
            return ImportResult.failed("requestUuid absent");
        }
        String requestUuid = request.getRequestUuid();
        try {
            // Idempotence : déjà importé ? (external_id = requestUuid)
            List<ElectronicOrder> existing = electronicOrderService.getElectronicOrdersByExternalId(requestUuid);
            if (existing != null && !existing.isEmpty()) {
                return ImportResult.alreadyPresent(existing.get(0).getId());
            }

            String serviceUserId = resolveServiceUserId();
            if (GenericValidator.isBlankOrNull(serviceUserId)) {
                // Sans utilisateur système, tous les inserts partiraient avec sys_user_id
                // null (colonnes NOT NULL/FK côté schéma) : on échoue clairement plutôt
                // que par demande, sans polluer la file d'ACK "FAILED".
                return ImportResult.failed("utilisateur système 'serviceUser' introuvable (import impossible)");
            }
            Patient patient = resolveOrCreatePatient(request, serviceUserId);
            Organization organization = resolveOrCreateOrganization(request, serviceUserId);

            ElectronicOrder eOrder = new ElectronicOrder();
            eOrder.setExternalId(requestUuid);
            eOrder.setData(rawJson);
            eOrder.setStatusId(statusService.getStatusID(ExternalOrderStatus.Entered));
            eOrder.setOrderTimestamp(nowOr(request.getAuthoredOn(), request.getRequestDate()));
            // Type FHIR : la demande provient d'un flux FHIR en amont du SC (comme la
            // réception mini-HIE). L'enum ElectronicOrderType ne définit que FHIR/HL7_V2.
            eOrder.setType(ElectronicOrderType.FHIR);
            eOrder.setPriority(OrderPriority.ROUTINE);
            eOrder.setSysUserId(serviceUserId);
            if (patient != null) {
                eOrder.setPatient(patient);
            }
            // Colonnes d'affichage dénormalisées (modèle 4a) — lecture de liste sans FHIR.
            eOrder.setCollectionDate(toTimestamp(request.getCollectionDate()));
            eOrder.setRequestingFacilityName(request.getRequestingSiteName());
            // testName laissé null : le SC ne fournit qu'un libellé 'charge virale'
            // générique, pas le test OE mappé (résolu à la saisie).
            // NB : l'entité ElectronicOrder de ce fork n'expose pas de setter
            // organization_id typé ; l'organisation est résolue/créée (dédup référentiel)
            // et son nom est porté par la colonne requesting_facility_name + le JSON data.
            // Le rattachement organisation↔ordre se fera à la création du sample.

            String eoId = electronicOrderService.insert(eOrder);
            return ImportResult.created(eoId);
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "importRequest",
                    "échec import demande " + requestUuid + " : " + e);
            return ImportResult.failed(e.getMessage());
        }
    }

    /**
     * Résolution/création patient : dédup par IDENTITÉ subject (table
     * patient_identity, PAS une propriété de Patient) puis par national id, puis
     * par external id ; création via les services person/patient/identity. Renvoie
     * null si aucun identifiant (pas de création possible).
     */
    private Patient resolveOrCreatePatient(EorderRequestDTO request, String serviceUserId) {
        String subject = request.getPatientSubjectNumber();
        String national = request.getPatientCode();

        // 1) par identité SUBJECT (subject vit dans patient_identity, pas sur Patient).
        Patient patient = findPatientBySubjectIdentity(subject);
        // 2) par national id.
        if (patient == null && !GenericValidator.isBlankOrNull(national)) {
            patient = patientService.getPatientByNationalId(national);
        }
        // 3) par external id (= subject stocké en external_id à la création).
        if (patient == null && !GenericValidator.isBlankOrNull(subject)) {
            patient = patientService.getPatientByExternalId(subject);
        }
        if (patient != null) {
            return patient;
        }
        if (GenericValidator.isBlankOrNull(subject) && GenericValidator.isBlankOrNull(national)) {
            // Aucun identifiant patient exploitable : on n'invente pas de patient.
            return null;
        }

        Person person = new Person();
        person.setLastName("");
        person.setFirstName("");
        person.setSysUserId(serviceUserId);

        patient = new Patient();
        patient.setPerson(person);
        patient.setGender(mapGender(request.getGender()));
        patient.setBirthDate(toTimestamp(request.getBirthDate()));
        patient.setNationalId(!GenericValidator.isBlankOrNull(national) ? national : subject);
        patient.setExternalId(!GenericValidator.isBlankOrNull(subject) ? subject : national);
        patient.setSysUserId(serviceUserId);

        personService.insert(person);
        patientService.insert(patient);

        if (!GenericValidator.isBlankOrNull(subject)) {
            addIdentity("SUBJECT", subject, patient.getId(), serviceUserId);
        }
        return patient;
    }

    /** Retrouve un patient par son identité SUBJECT (via patient_identity). */
    private Patient findPatientBySubjectIdentity(String subject) {
        if (GenericValidator.isBlankOrNull(subject)) {
            return null;
        }
        PatientIdentityType subjectType = identityTypeService.getNamedIdentityType("SUBJECT");
        if (subjectType == null) {
            return null;
        }
        List<PatientIdentity> matches = identityService.getPatientIdentitiesByValueAndType(subject,
                subjectType.getId());
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        String patientId = matches.get(0).getPatientId();
        return GenericValidator.isBlankOrNull(patientId) ? null : patientService.get(patientId);
    }

    private void addIdentity(String typeName, String value, String patientId, String serviceUserId) {
        PatientIdentityType type = identityTypeService.getNamedIdentityType(typeName);
        if (type == null) {
            return;
        }
        PatientIdentity identity = new PatientIdentity();
        identity.setIdentityTypeId(type.getId());
        identity.setPatientId(patientId);
        identity.setIdentityData(value);
        identity.setSysUserId(serviceUserId);
        identityService.insert(identity);
    }

    /**
     * Résolution/création organisation demandeuse : dédup par short_name puis name.
     * Création minimale (name + short_name + active) si absente.
     */
    private Organization resolveOrCreateOrganization(EorderRequestDTO request, String serviceUserId) {
        String code = request.getRequestingSiteCode();
        String name = request.getRequestingSiteName();
        if (GenericValidator.isBlankOrNull(code) && GenericValidator.isBlankOrNull(name)) {
            return null;
        }
        Organization org = null;
        if (!GenericValidator.isBlankOrNull(code)) {
            org = organizationService.getOrganizationByShortName(code, true);
        }
        if (org == null && !GenericValidator.isBlankOrNull(name)) {
            Organization probe = new Organization();
            probe.setOrganizationName(name);
            org = organizationService.getOrganizationByName(probe, true);
        }
        if (org != null) {
            return org;
        }
        org = new Organization();
        org.setOrganizationName(!GenericValidator.isBlankOrNull(name) ? name : code);
        org.setShortName(code);
        org.setIsActive("Y");
        org.setSysUserId(serviceUserId);
        organizationService.insert(org);
        return org;
    }

    private String resolveServiceUserId() {
        SystemUser serviceUser = systemUserService.getDataForLoginUser("serviceUser");
        return serviceUser == null ? null : serviceUser.getId();
    }

    private static String mapGender(String gender) {
        if (GenericValidator.isBlankOrNull(gender)) {
            return null;
        }
        String g = gender.trim().toLowerCase();
        if (g.startsWith("m")) {
            return "M";
        }
        if (g.startsWith("f")) {
            return "F";
        }
        return null;
    }

    private static Timestamp nowOr(LocalDateTime authoredOn, LocalDate requestDate) {
        if (authoredOn != null) {
            return Timestamp.valueOf(authoredOn);
        }
        if (requestDate != null) {
            return Timestamp.valueOf(requestDate.atStartOfDay());
        }
        return new Timestamp(System.currentTimeMillis());
    }

    private static Timestamp toTimestamp(LocalDateTime ldt) {
        return ldt == null ? null : Timestamp.valueOf(ldt);
    }

    private static Timestamp toTimestamp(LocalDate ld) {
        return ld == null ? null : Timestamp.valueOf(ld.atStartOfDay());
    }

    @SuppressWarnings("unused")
    private static ZoneId zone() {
        return ZoneId.systemDefault();
    }
}
