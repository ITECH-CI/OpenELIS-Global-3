package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FhirPersistanceServiceImpl implements FhirPersistanceService {

    public static class FhirOperations {
        // theses are maps so you can see if the resources you're creating will collide,
        // the map key is not used otherwise
        Map<String, Resource> createResources; // will do a put with a new uuid
        Map<String, Resource> updateResources; // will do a put with the id used in the resource
        // key = "ResourceType/id" ; entry.request DELETE. Sert notamment à purger les
        // anciens DiagnosticReport par-analyse remplacés par un DR groupé par bon.
        Map<String, Resource> deleteResources;

        public FhirOperations() {
            createResources = new HashMap<>();
            updateResources = new HashMap<>();
            deleteResources = new HashMap<>();
        }

        public FhirOperations(int createSize, int updateSize) {
            createResources = new HashMap<>(createSize);
            updateResources = new HashMap<>(updateSize);
            deleteResources = new HashMap<>();
        }

        public void addAll(FhirOperations fhirOperationLists) {
            createResources.putAll(fhirOperationLists.createResources);
            updateResources.putAll(fhirOperationLists.updateResources);
            deleteResources.putAll(fhirOperationLists.deleteResources);
        }
    }

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirContext fhirContext;

    IGenericClient localFhirClient;

    @PostConstruct
    public void init() {
        if (StringUtils.isNotBlank(fhirConfig.getLocalFhirStorePath())) {
            localFhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
        }
    }

    @Override
    public Bundle createFhirResourceInFhirStore(Resource resource) throws FhirLocalPersistingException {
        String id = resource.getIdElement().getIdPart() == null ? "" : resource.getIdElement().getIdPart();
        return createFhirResourcesInFhirStore(Map.of(id, resource));
    }

    @Override
    public Bundle updateFhirResourceInFhirStore(Resource resource) throws FhirLocalPersistingException {
        return updateFhirResourcesInFhirStore(Map.of(resource.getIdElement().getIdPart(), resource));
    }

    @Override
    public Bundle createFhirResourcesInFhirStore(Map<String, Resource> resources) throws FhirLocalPersistingException {
        Bundle transactionBundle = makeTransactionBundleForCreate(resources);
        Bundle transactionResponseBundle = new Bundle();
        try {
            transactionResponseBundle = localFhirClient.transaction().withBundle(transactionBundle).execute();
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new FhirLocalPersistingException(e);
        }
        verifyResponseBundle(transactionResponseBundle);
        return transactionResponseBundle;
    }

    @Override
    public Bundle updateFhirResourcesInFhirStore(Map<String, Resource> resources) throws FhirLocalPersistingException {
        Bundle transactionBundle = makeTransactionBundleForUpdate(resources);
        Bundle transactionResponseBundle = new Bundle();
        try {
            transactionResponseBundle = localFhirClient.transaction().withBundle(transactionBundle).execute();
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new FhirLocalPersistingException(e);
        }
        verifyResponseBundle(transactionResponseBundle);
        return transactionResponseBundle;
    }

    @Override
    public Bundle createUpdateFhirResourcesInFhirStore(Map<String, Resource> createResources,
            Map<String, Resource> updateResources) throws FhirLocalPersistingException {
        Bundle transactionBundle = new Bundle();
        transactionBundle.setType(BundleType.TRANSACTION);
        addUpdatesToTransactionBundle(updateResources, transactionBundle);
        addCreateToTransactionBundle(createResources, transactionBundle);
        Bundle transactionResponseBundle = new Bundle();
        try {
            LogEvent.logTrace(this.getClass().getSimpleName(), "",
                    "creating resources: " + fhirContext.newJsonParser().encodeResourceToString(transactionBundle));
            transactionResponseBundle = localFhirClient.transaction().withBundle(transactionBundle).execute();
            LogEvent.logTrace(this.getClass().getSimpleName(), "", "created resources: "
                    + fhirContext.newJsonParser().encodeResourceToString(transactionResponseBundle));
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new FhirLocalPersistingException(e);
        }
        verifyResponseBundle(transactionResponseBundle);
        return transactionResponseBundle;
    }

    @Override
    public Bundle createUpdateFhirResourcesInFhirStore(FhirOperations fhirOperations)
            throws FhirLocalPersistingException {
        List<FhirOperations> fhirOperationsList = Arrays.asList(fhirOperations);
        return createUpdateFhirResourcesInFhirStore(fhirOperationsList);
    }

    @Override
    public Bundle createUpdateFhirResourcesInFhirStore(List<FhirOperations> fhirOperationsList)
            throws FhirLocalPersistingException {
        Bundle transactionBundle = new Bundle();
        transactionBundle.setType(BundleType.TRANSACTION);
        for (FhirOperations fhirOperations : fhirOperationsList) {
            // DELETE d'abord : purge des ressources obsolètes (ex. anciens DR
            // par-analyse) avant d'écrire les nouvelles. Ids disjoints des create/update,
            // mais l'ordre DELETE→write reste le plus sûr.
            addDeletesToTransactionBundle(fhirOperations.deleteResources, transactionBundle);
            addCreateToTransactionBundle(fhirOperations.createResources, transactionBundle);
            addUpdatesToTransactionBundle(fhirOperations.updateResources, transactionBundle);
        }
        Bundle transactionResponseBundle = new Bundle();
        try {
            LogEvent.logTrace(this.getClass().getSimpleName(), "",
                    "creating resources: " + fhirContext.newJsonParser().encodeResourceToString(transactionBundle));
            transactionResponseBundle = localFhirClient.transaction().withBundle(transactionBundle).execute();
            LogEvent.logTrace(this.getClass().getSimpleName(), "", "created resources: "
                    + fhirContext.newJsonParser().encodeResourceToString(transactionResponseBundle));
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new FhirLocalPersistingException(e);
        }
        verifyResponseBundle(transactionResponseBundle);
        return transactionResponseBundle;
    }

    // Une transaction FHIR peut renvoyer 200 avec des entrées en ERREUR partielle.
    // Sans cette vérif, une persistance FHIR partiellement échouée passait pour un
    // succès (fhirUuid marqué en base alors que la ressource n'existe pas côté
    // store). On inspecte le statut de chaque entrée du bundle réponse : un statut
    // qui ne commence pas par "2" (2xx) est un échec -> on lève une exception, que
    // le listener trace en FAILED et rend rejouable.
    private void verifyResponseBundle(Bundle responseBundle) throws FhirLocalPersistingException {
        if (responseBundle == null || responseBundle.getEntry() == null) {
            return;
        }
        for (BundleEntryComponent entry : responseBundle.getEntry()) {
            if (entry.getResponse() != null && entry.getResponse().hasStatus()) {
                String status = entry.getResponse().getStatus();
                if (status != null && !status.trim().startsWith("2")) {
                    String msg = "FHIR transaction entry failed with status: " + status;
                    LogEvent.logError(this.getClass().getSimpleName(), "verifyResponseBundle", msg);
                    throw new FhirLocalPersistingException(msg);
                }
            }
        }
    }

    @Override
    public List<ServiceRequest> getAllServiceRequestByAccessionNumber(String accessionNumber) {
        List<ServiceRequest> serviceRequests = new ArrayList<>();
        Bundle searchBundle = localFhirClient.search().forResource(ServiceRequest.class) //
                .returnBundle(Bundle.class) //
                .where(ServiceRequest.IDENTIFIER.exactly()
                        .systemAndIdentifier(fhirConfig.getOeFhirSystem() + "/samp_labNo", accessionNumber)) //
                .execute();
        for (BundleEntryComponent entry : searchBundle.getEntry()) {
            serviceRequests.add((ServiceRequest) entry.getResource());
        }
        while (searchBundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            searchBundle = localFhirClient.loadPage().next(searchBundle).execute();
            for (BundleEntryComponent entry : searchBundle.getEntry()) {
                serviceRequests.add((ServiceRequest) entry.getResource());
            }
        }
        return serviceRequests;
    }

    @Override
    public Optional<Organization> getFhirOrganizationByName(String orgName) {
        Bundle bundle = localFhirClient.search() //
                .forResource(Organization.class) //
                .returnBundle(Bundle.class) //
                .where(Organization.NAME.matchesExactly().value(orgName)) //
                .execute();
        if (bundle.hasEntry()) {
            return Optional.of((Organization) bundle.getEntryFirstRep().getResource());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Patient> getPatientByUuid(String uuid) {
        Bundle bundle = localFhirClient.search() //
                .forResource(Patient.class) //
                .returnBundle(Bundle.class) //
                .where(Patient.IDENTIFIER.exactly().systemAndIdentifier(fhirConfig.getOeFhirSystem() + "/pat_uuid",
                        uuid)) //
                .execute();
        if (bundle.hasEntry()) {
            return Optional.of((Patient) bundle.getEntryFirstRep().getResource());
        }
        return Optional.empty();
    }

    @Override
    public Optional<ServiceRequest> getServiceRequestByAnalysisUuid(String uuid) {
        Bundle bundle = localFhirClient.search() //
                .forResource(ServiceRequest.class) //
                .returnBundle(Bundle.class) //
                .where(ServiceRequest.IDENTIFIER.exactly()
                        .systemAndIdentifier(fhirConfig.getOeFhirSystem() + "/analysis_uuid", uuid)) //
                .execute();
        if (bundle.hasEntry()) {
            return Optional.of((ServiceRequest) bundle.getEntryFirstRep().getResource());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Specimen> getSpecimenBySampleItemUuid(String uuid) {
        Bundle bundle = localFhirClient.search() //
                .forResource(Specimen.class) //
                .returnBundle(Bundle.class) //
                .where(Specimen.IDENTIFIER.exactly()
                        .systemAndIdentifier(fhirConfig.getOeFhirSystem() + "/sampleItem_uuid", uuid)) //
                .execute();
        if (bundle.hasEntry()) {
            return Optional.of((Specimen) bundle.getEntryFirstRep().getResource());
        }
        return Optional.empty();
    }

    @Override
    public Optional<DiagnosticReport> getDiagnosticReportByAnalysisUuid(String uuid) {
        Bundle bundle = localFhirClient.search() //
                .forResource(DiagnosticReport.class) //
                .returnBundle(Bundle.class) //
                .where(DiagnosticReport.IDENTIFIER.exactly()
                        .systemAndIdentifier(fhirConfig.getOeFhirSystem() + "/analysisResult_uuid", uuid)) //
                .execute();
        if (bundle.hasEntry()) {
            return Optional.of((DiagnosticReport) bundle.getEntryFirstRep().getResource());
        }
        return Optional.empty();
    }

    @Override
    public Bundle makeTransactionBundleForCreate(Map<String, Resource> resources) {
        Bundle transactionBundle = new Bundle();
        transactionBundle.setType(BundleType.TRANSACTION);
        addCreateToTransactionBundle(resources, transactionBundle);
        return transactionBundle;
    }

    public Bundle makeTransactionBundleForUpdate(Map<String, Resource> resources) {
        Bundle transactionBundle = new Bundle();
        transactionBundle.setType(BundleType.TRANSACTION);
        addUpdatesToTransactionBundle(resources, transactionBundle);
        return transactionBundle;
    }

    public Bundle addCreateToTransactionBundle(Map<String, Resource> createResources, Bundle transactionBundle) {
        for (Resource resource : createResources.values()) {
            String id = UUID.randomUUID().toString();
            String resourceType = resource.getResourceType().toString();
            if (ResourceType.Patient.toString().equalsIgnoreCase(resourceType)) {
                Patient patient = (Patient) resource;
                if (!patient.getIdentifier().stream()
                        .anyMatch(e -> e.getSystem().equals(fhirConfig.getOeFhirSystem() + "/pat_uuid"))) {
                    patient.addIdentifier(
                            new Identifier().setValue(id).setSystem(fhirConfig.getOeFhirSystem() + "/pat_uuid"));
                }
            }
            transactionBundle.addEntry().setFullUrl(resource.getIdElement().getValue()).setResource(resource)
                    .getRequest().setUrl(resourceType + "/" + id).setMethod(Bundle.HTTPVerb.PUT);
        }
        return transactionBundle;
    }

    public Bundle addUpdatesToTransactionBundle(Map<String, Resource> updateResources, Bundle transactionBundle) {
        for (Resource resource : updateResources.values()) {
            transactionBundle.addEntry().setFullUrl(resource.getIdElement().getValue()).setResource(resource)
                    .getRequest().setUrl(resource.getResourceType() + "/" + resource.getIdElement().getIdPart())
                    .setMethod(Bundle.HTTPVerb.PUT);
        }
        return transactionBundle;
    }

    // Ajoute un entry DELETE par ressource (aucun corps, juste request.url =
    // "ResourceType/id"). Utilisé pour purger des ressources devenues obsolètes
    // dans
    // la même transaction que l'écriture des nouvelles.
    public Bundle addDeletesToTransactionBundle(Map<String, Resource> deleteResources, Bundle transactionBundle) {
        for (Resource resource : deleteResources.values()) {
            transactionBundle.addEntry().getRequest()
                    .setUrl(resource.getResourceType() + "/" + resource.getIdElement().getIdPart())
                    .setMethod(Bundle.HTTPVerb.DELETE);
        }
        return transactionBundle;
    }

    @Override
    public Optional<Task> getTaskBasedOnServiceRequest(String referringId) {

        Bundle bundle = localFhirClient.search() //
                .forResource(Task.class) //
                .returnBundle(Bundle.class) //
                .where(Task.BASED_ON.hasId(referringId)) //
                .execute();
        if (bundle.hasEntry()) {
            return Optional.of((Task) bundle.getEntryFirstRep().getResource());
        }
        LogEvent.logDebug(FhirPersistanceServiceImpl.class.getName(), "getTaskBasedOnServiceRequest",
                "no task found based-on serviceRequest " + referringId);
        LogEvent.logDebug(FhirPersistanceServiceImpl.class.getName(), "getTaskBasedOnServiceRequest",
                "trying to find service requests that have an identifier equal to " + referringId);

        ServiceRequest serviceRequest = null;

        bundle = localFhirClient.search() //
                .forResource(ServiceRequest.class) //
                .returnBundle(Bundle.class) //
                .where(ServiceRequest.IDENTIFIER.exactly().identifier(referringId)) //
                .execute();
        if (bundle.hasEntry()) {
            serviceRequest = (ServiceRequest) bundle.getEntryFirstRep().getResource();
        }
        if (serviceRequest == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "", "no service request with identifier " + referringId);
            for (String remotePath : fhirConfig.getRemoteStorePaths()) {

                bundle = localFhirClient.search() //
                        .forResource(ServiceRequest.class) //
                        .returnBundle(Bundle.class) //
                        .where(ServiceRequest.IDENTIFIER.exactly().systemAndIdentifier(remotePath, referringId)) //
                        .execute();
                if (bundle.hasEntry()) {
                    serviceRequest = (ServiceRequest) bundle.getEntryFirstRep().getResource();
                }
            }
        }
        if (serviceRequest == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "",
                    "no service request with identifier " + referringId + " with configured systems");
        } else {
            LogEvent.logDebug(FhirPersistanceServiceImpl.class.getName(), "getTaskBasedOnServiceRequest",
                    "found serviceRequest " + serviceRequest.getIdElement().getIdPart()
                            + " with an equal identifier to " + referringId);
            bundle = localFhirClient.search() //
                    .forResource(Task.class) //
                    .returnBundle(Bundle.class) //
                    .where(Task.BASED_ON.hasId(serviceRequest.getIdElement().getIdPart())) //
                    .execute();
            if (bundle.hasEntry()) {
                return Optional.of((Task) bundle.getEntryFirstRep().getResource());
            }
            LogEvent.logDebug(FhirPersistanceServiceImpl.class.getName(), "getTaskBasedOnServiceRequest",
                    "no task found based-on serviceRequest " + serviceRequest.getIdElement().getIdPart());
        }

        return Optional.empty();
    }

    @Override
    public Optional<ServiceRequest> getServiceRequestByReferingId(String referringId) {
        ServiceRequest serviceRequest = null;
        Bundle bundle = localFhirClient.search() //
                .forResource(ServiceRequest.class) //
                .returnBundle(Bundle.class) //
                .where(ServiceRequest.IDENTIFIER.exactly().identifier(referringId)) //
                .execute();
        if (bundle.hasEntry()) {
            serviceRequest = (ServiceRequest) bundle.getEntryFirstRep().getResource();
        }
        if (serviceRequest == null) {
            LogEvent.logDebug(this.getClass().getName(), "", "no service request with identifier " + referringId);
            for (String remotePath : fhirConfig.getRemoteStorePaths()) {

                bundle = localFhirClient.search() //
                        .forResource(ServiceRequest.class) //
                        .returnBundle(Bundle.class) //
                        .where(ServiceRequest.IDENTIFIER.exactly().systemAndIdentifier(remotePath, referringId)) //
                        .execute();
                if (bundle.hasEntry()) {
                    serviceRequest = (ServiceRequest) bundle.getEntryFirstRep().getResource();
                }
            }
        }
        if (serviceRequest == null) {
            LogEvent.logDebug(this.getClass().getName(), "", "no service request with identifier " + referringId);

            bundle = localFhirClient.search() //
                    .forResource(ServiceRequest.class) //
                    .returnBundle(Bundle.class) //
                    .where(ServiceRequest.RES_ID.exactly().code(referringId)).execute();
            if (bundle.hasEntry()) {
                serviceRequest = (ServiceRequest) bundle.getEntryFirstRep().getResource();
            }
        }
        if (serviceRequest == null) {
            LogEvent.logDebug(this.getClass().getName(), "",
                    "no service request with identifier " + referringId + " with configured systems");
        } else {
            return Optional.of(serviceRequest);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Task> getTaskBasedOnTask(String taskId) {
        Bundle bundle = localFhirClient.search() //
                .forResource(Task.class) //
                .returnBundle(Bundle.class) //
                .where(Task.BASED_ON.hasId(taskId)) //
                .execute();
        if (bundle.hasEntry()) {
            return Optional.of((Task) bundle.getEntryFirstRep().getResource());
        }

        LogEvent.logDebug(FhirPersistanceServiceImpl.class.getName(), "getTaskBasedOnTask",
                "no task found based-on task " + taskId);
        LogEvent.logDebug(FhirPersistanceServiceImpl.class.getName(), "getTaskBasedOnTask",
                "trying to find task that have an identifier equal to " + taskId);

        Task task = null;

        bundle = localFhirClient.search() //
                .forResource(Task.class) //
                .returnBundle(Bundle.class) //
                .where(Task.IDENTIFIER.exactly().identifier(taskId)) //
                .execute();
        if (bundle.hasEntry()) {
            task = (Task) bundle.getEntryFirstRep().getResource();
        }
        if (task == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "", "no task with identifier " + taskId);
            for (String remotePath : fhirConfig.getRemoteStorePaths()) {

                bundle = localFhirClient.search() //
                        .forResource(Task.class) //
                        .returnBundle(Bundle.class) //
                        .where(Task.IDENTIFIER.exactly().systemAndIdentifier(remotePath, taskId)) //
                        .execute();
                if (bundle.hasEntry()) {
                    task = (Task) bundle.getEntryFirstRep().getResource();
                }
            }
        }
        if (task == null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "",
                    "no task with identifier " + taskId + " with configured systems");
        } else {
            LogEvent.logDebug(FhirPersistanceServiceImpl.class.getName(), "getTaskBasedOnTask",
                    "found task " + task.getIdElement().getIdPart() + " with an equal identifier to " + taskId);
            bundle = localFhirClient.search() //
                    .forResource(Task.class) //
                    .returnBundle(Bundle.class) //
                    .where(Task.BASED_ON.hasId(task.getIdElement().getIdPart())) //
                    .execute();
            if (bundle.hasEntry()) {
                return Optional.of((Task) bundle.getEntryFirstRep().getResource());
            }
            LogEvent.logWarn(FhirPersistanceServiceImpl.class.getName(), "getTaskBasedOnTask",
                    "no task found based-on task" + task.getIdElement().getIdPart());
        }

        return Optional.empty();
    }
}
