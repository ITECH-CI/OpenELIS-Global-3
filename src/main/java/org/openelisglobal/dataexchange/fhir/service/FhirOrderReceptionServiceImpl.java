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

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.dataexchange.fhir.service.TaskWorker.TaskResult;
import org.openelisglobal.dataexchange.order.action.DBOrderExistanceChecker;
import org.openelisglobal.dataexchange.order.action.IOrderPersister;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FhirOrderReceptionServiceImpl implements FhirOrderReceptionService {

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Autowired
    private FhirConfig fhirConfig;

    @Override
    @Transactional
    public ReceptionResult receiveOrderBundle(String bundleJson) {
        if (bundleJson == null || bundleJson.trim().isEmpty()) {
            return new ReceptionResult(false, null, "corps vide");
        }

        Bundle bundle;
        try {
            bundle = fhirUtil.getFhirParser().parseResource(Bundle.class, bundleJson);
        } catch (Exception e) {
            return new ReceptionResult(false, null, "Bundle FHIR invalide : " + e.getMessage());
        }

        // Extraction des ressources du Bundle par type.
        Patient patient = null;
        List<ServiceRequest> serviceRequests = new ArrayList<>();
        List<QuestionnaireResponse> questionnaireResponses = new ArrayList<>();
        List<Specimen> specimens = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Resource resource = entry.getResource();
            if (resource == null) {
                continue;
            }
            if (resource.getResourceType() == ResourceType.Patient && patient == null) {
                patient = (Patient) resource;
            } else if (resource.getResourceType() == ResourceType.ServiceRequest) {
                serviceRequests.add((ServiceRequest) resource);
            } else if (resource.getResourceType() == ResourceType.QuestionnaireResponse) {
                questionnaireResponses.add((QuestionnaireResponse) resource);
            } else if (resource.getResourceType() == ResourceType.Specimen) {
                specimens.add((Specimen) resource);
            }
        }

        if (patient == null) {
            return new ReceptionResult(false, null, "Patient absent du Bundle");
        }
        if (serviceRequests.isEmpty()) {
            return new ReceptionResult(false, null, "ServiceRequest absent du Bundle");
        }

        // 1) Attribution d'ids locaux stables et persistance dans le store FHIR local
        // (Patient/ServiceRequest/QR/Specimen), pour que la reprise en entrée
        // d'échantillon (LabOrderSearchProvider) retrouve ces ressources.
        FhirOperations fhirOperations = new FhirOperations();

        ensureId(patient);
        // Tag le Patient d'un identifiant "externalId" reconnu par l'interpréteur natif
        // (TaskInterpreterImpl : type coding genIdType/externalId). Sans lui, le
        // Patient
        // reçu d'un tiers n'a aucun identifiant OE et la reprise d'ordre échoue (le
        // persister interroge la BD avec un externalId null). On réutilise l'UUID local
        // du Patient comme externalId, comme le fait le flux d'échange natif
        // (FhirApiWorkFlowServiceImpl#createIdentifierToRemoteResource).
        ensureExternalIdIdentifier(patient);
        fhirOperations.updateResources.put(patient.getIdElement().getIdPart(), patient);

        // On mémorise l'id ORIGINAL de chaque ServiceRequest (celui référencé par le
        // tiers dans QR.basedOn) avant de lui attribuer un id local, pour pouvoir
        // réécrire les liens QR -> SR sur les nouveaux ids.
        Map<String, String> srIdRemap = new HashMap<>();
        for (ServiceRequest sr : serviceRequests) {
            String originalId = sr.getIdElement() != null ? sr.getIdElement().getIdPart() : null;
            // On aligne l'id local du ServiceRequest sur SON NUMÉRO D'ORDRE (identifier),
            // comme le fait le flux d'import natif : c'est par cet id (RES_ID) que la
            // reprise en entrée d'échantillon (LabOrderSearchProvider) retrouve le SR à
            // partir du numéro de l'ElectronicOrder. Sans ça, le SR reçu est introuvable
            // et la reprise plante (serviceRequest null).
            String orderNumber = sr.hasIdentifier() ? sr.getIdentifierFirstRep().getValue() : null;
            if (!GenericValidator.isBlankOrNull(orderNumber)) {
                sr.setId(orderNumber);
            } else {
                ensureId(sr);
            }
            if (originalId != null && !originalId.isEmpty()) {
                srIdRemap.put(originalId, sr.getIdElement().getIdPart());
            }
            // Rattache le ServiceRequest au patient reçu.
            sr.setSubject(referenceTo(ResourceType.Patient, patient.getIdElement().getIdPart()));
            fhirOperations.updateResources.put(sr.getIdElement().getIdPart(), sr);
        }
        for (Specimen sp : specimens) {
            ensureId(sp);
            fhirOperations.updateResources.put(sp.getIdElement().getIdPart(), sp);
        }
        // Les QR portent les renseignements cliniques additionnels. On les rattache au
        // ServiceRequest via based-on (lien exploité à la reprise en entrée
        // d'échantillon, cf LabOrderSearchProvider) : soit le tiers a fourni un basedOn
        // (qu'on réécrit sur le nouvel id local), soit on rattache au SR unique du
        // Bundle. Sans ce lien, le QR est stocké orphelin et jamais réexploité.
        String soleServiceRequestId = serviceRequests.size() == 1 ? serviceRequests.get(0).getIdElement().getIdPart()
                : null;
        for (QuestionnaireResponse qr : questionnaireResponses) {
            ensureId(qr);
            rewireQuestionnaireResponseBasedOn(qr, srIdRemap, soleServiceRequestId);
            fhirOperations.updateResources.put(qr.getIdElement().getIdPart(), qr);
        }

        try {
            fhirPersistanceService.createUpdateFhirResourcesInFhirStore(fhirOperations);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "receiveOrderBundle",
                    "échec persistance FHIR store : " + e);
            return new ReceptionResult(false, null, "échec de stockage des ressources FHIR : " + e.getMessage());
        }

        // 2) Pour chaque ServiceRequest : Task minimal (porteur de liens) + moteur
        // d'ordre unique (TaskWorker) -> crée un ElectronicOrder "en attente".
        int accepted = 0;
        StringBuilder details = new StringBuilder();
        String firstOrderNumber = null;
        for (ServiceRequest sr : serviceRequests) {
            try {
                Task minimalTask = new Task();
                minimalTask.setId(UUID.randomUUID().toString());
                minimalTask.setStatus(TaskStatus.REQUESTED);
                minimalTask.setFor(referenceTo(ResourceType.Patient, patient.getIdElement().getIdPart()));
                minimalTask.addBasedOn(referenceTo(ResourceType.ServiceRequest, sr.getIdElement().getIdPart()));

                TaskWorker worker = new TaskWorker(minimalTask,
                        fhirContext.newJsonParser().encodeResourceToString(minimalTask), sr, patient);
                worker.setInterpreter(SpringContext.getBean(TaskInterpreter.class));
                worker.setExistanceChecker(SpringContext.getBean(DBOrderExistanceChecker.class));
                worker.setPersister(SpringContext.getBean(IOrderPersister.class));

                TaskResult result = worker.handleOrderRequest();
                if (firstOrderNumber == null) {
                    firstOrderNumber = sr.hasIdentifier() ? sr.getIdentifierFirstRep().getValue() : null;
                }
                if (result == TaskResult.OK) {
                    // Persiste le Task (based-on SR) dans le store FHIR local : la reprise
                    // en entrée d'échantillon (LabOrderSearchProvider) le recherche par
                    // based-on. Le flux natif a toujours un Task présent ; l'ordre reçu en
                    // PUSH doit donc aussi le matérialiser. En cas d'échec on PROPAGE :
                    // l'ElectronicOrder vient d'être créé dans la même transaction, il faut
                    // tout annuler plutôt que de laisser un ordre irrécupérable (sans Task,
                    // la reprise plante sur orElseThrow).
                    FhirOperations taskOps = new FhirOperations();
                    taskOps.updateResources.put(minimalTask.getIdElement().getIdPart(), minimalTask);
                    fhirPersistanceService.createUpdateFhirResourcesInFhirStore(taskOps);
                    accepted++;
                } else {
                    details.append(sr.getIdElement().getIdPart()).append('=').append(result).append(' ');
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "receiveOrderBundle",
                        "échec traitement ServiceRequest : " + e);
                details.append("erreur ").append(e.getMessage()).append(' ');
            }
        }

        if (accepted > 0) {
            return new ReceptionResult(true, firstOrderNumber,
                    accepted + " ordre(s) reçu(s)" + (details.length() > 0 ? " ; " + details : ""));
        }
        return new ReceptionResult(false, firstOrderNumber,
                "aucun ordre accepté" + (details.length() > 0 ? " : " + details : ""));
    }

    /**
     * Garantit que le Patient porte un identifiant "externalId" au format attendu
     * par TaskInterpreterImpl (type coding {@code <oeFhirSystem>/genIdType} =
     * externalId). Idempotent : ne rajoute rien si un tel identifiant est déjà
     * présent. La valeur est l'UUID local du Patient (déjà attribué par
     * {@link #ensureId}).
     */
    private void ensureExternalIdIdentifier(Patient patient) {
        String genIdSystem = fhirConfig.getOeFhirSystem() + "/genIdType";
        for (Identifier existing : patient.getIdentifier()) {
            if (existing.hasType() && existing.getType().hasCoding(genIdSystem, "externalId")) {
                return;
            }
        }
        Identifier identifier = new Identifier();
        identifier.setSystem(fhirConfig.getLocalFhirStorePath());
        identifier.setType(new CodeableConcept().addCoding(new Coding().setCode("externalId").setSystem(genIdSystem)));
        identifier.setValue(patient.getIdElement().getIdPart());
        patient.addIdentifier(identifier);
    }

    /**
     * Garantit que le QuestionnaireResponse pointe (via based-on) vers le
     * ServiceRequest local. Si le tiers a fourni un based-on, on réécrit sa
     * référence sur le nouvel id local (via {@code srIdRemap}). Sinon, si le Bundle
     * ne contient qu'un seul ServiceRequest, on rattache le QR à celui-ci.
     * Idempotent.
     */
    private void rewireQuestionnaireResponseBasedOn(QuestionnaireResponse qr, Map<String, String> srIdRemap,
            String soleServiceRequestId) {
        boolean linked = false;
        for (Reference ref : qr.getBasedOn()) {
            if (ref.getReferenceElement() == null || ref.getReferenceElement().getIdPart() == null) {
                continue;
            }
            String referencedId = ref.getReferenceElement().getIdPart();
            String newId = srIdRemap.get(referencedId);
            if (newId != null) {
                ref.setReference(ResourceType.ServiceRequest.name() + "/" + newId);
                linked = true;
            }
        }
        if (!linked && soleServiceRequestId != null) {
            qr.addBasedOn(referenceTo(ResourceType.ServiceRequest, soleServiceRequestId));
        }
    }

    private void ensureId(Resource resource) {
        if (resource.getIdElement() == null || resource.getIdElement().getIdPart() == null
                || resource.getIdElement().getIdPart().isEmpty()) {
            resource.setId(UUID.randomUUID().toString());
        }
    }

    private Reference referenceTo(ResourceType type, String id) {
        return new Reference(type.name() + "/" + id);
    }
}
