package org.openelisglobal.dataexchange.service.order;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskPriority;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderCategorie;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderOrigine;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalOrderEntryServiceImpl implements LocalOrderEntryService {

    /** Préfixe des demandes saisies localement. */
    public static final String LOCAL_TASK_PREFIX = "local-";
    /** Contenu de la Task : le prescripteur saisi. */
    public static final String CONTAINED_PRESCRIBER_ID = "prescripteur";

    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private TestService testService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirConfig fhirConfig;

    /** Système de l'identifiant d'un examen saisi localement (Task.focus). */
    public static String testIdSystem(FhirConfig config) {
        return config.getOeFhirSystem() + "/test_id";
    }

    @Override
    @Transactional
    public String create(LocalOrderForm form, String sysUserId) {
        if (form == null) {
            throw new IllegalArgumentException("demande vide");
        }
        Patient patient = GenericValidator.isBlankOrNull(form.patientId) ? null : patientService.get(form.patientId);
        if (patient == null) {
            throw new IllegalArgumentException("patient introuvable");
        }
        Set<String> testIds = new LinkedHashSet<>();
        if (form.testIds != null) {
            form.testIds.stream().filter(id -> !GenericValidator.isBlankOrNull(id)).map(String::trim)
                    .forEach(testIds::add);
        }
        if (testIds.isEmpty()) {
            throw new IllegalArgumentException("aucun examen demandé");
        }
        List<Test> tests = new ArrayList<>();
        for (String testId : testIds) {
            Test test = testService.get(testId);
            if (test == null || !"Y".equals(test.getIsActive())) {
                throw new IllegalArgumentException("examen inconnu ou inactif : " + testId);
            }
            tests.add(test);
        }
        Organization organization = null;
        if (!GenericValidator.isBlankOrNull(form.requestingOrganizationId)) {
            organization = organizationService.get(form.requestingOrganizationId);
            if (organization == null) {
                throw new IllegalArgumentException("établissement demandeur inconnu");
            }
        }
        String venue = form.venueNumber == null ? "" : form.venueNumber.trim();
        if (venue.length() > 64) {
            throw new IllegalArgumentException("numéro de venue trop long (64 caractères au plus)");
        }
        OrderPriority priority = priorityOf(form.priority);

        String taskId = LOCAL_TASK_PREFIX + UUID.randomUUID();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        int index = 0;
        for (Test test : tests) {
            index++;
            ElectronicOrder eOrder = new ElectronicOrder();
            eOrder.setExternalId(taskId + "-" + index);
            eOrder.setData(fhirUtil.getFhirParser().encodeResourceToString(
                    localTask(taskId, now, patient, organization, form, venue, priority, test)));
            eOrder.setStatusId(statusService.getStatusID(ExternalOrderStatus.Entered));
            eOrder.setOrderTimestamp(now);
            eOrder.setType(ElectronicOrderType.FHIR);
            eOrder.setPriority(priority);
            eOrder.setPatient(patient);
            eOrder.setCategorie(ElectronicOrderCategorie.ROUTINE);
            eOrder.setOrigine(ElectronicOrderOrigine.SAISIE_LOCALE);
            eOrder.setTestName(test.getLocalizedTestName() != null ? test.getLocalizedTestName().getLocalizedValue()
                    : test.getName());
            if (organization != null) {
                eOrder.setRequestingFacilityName(organization.getOrganizationName());
                eOrder.setOrganizationId(organization.getId());
            }
            eOrder.setSysUserId(sysUserId);
            electronicOrderService.insert(eOrder);
        }
        return taskId;
    }

    // Task locale (jamais écrite dans le serveur FHIR) : prescripteur contenu,
    // établissement en restriction.recipient, examen en focus, numéro de venue en
    // encounter.identifier.
    Task localTask(String taskId, Timestamp now, Patient patient, Organization organization, LocalOrderForm form,
            String venue, OrderPriority priority, Test test) {
        Task task = new Task();
        task.setId(taskId);
        task.setStatus(TaskStatus.REQUESTED);
        task.setIntent(TaskIntent.ORDER);
        task.setAuthoredOn(now);
        task.setPriority(priority == OrderPriority.STAT ? TaskPriority.STAT
                : priority == OrderPriority.ASAP ? TaskPriority.ASAP : TaskPriority.ROUTINE);
        if (patient.getFhirUuid() != null) {
            task.setFor(new Reference("Patient/" + patient.getFhirUuidAsString()));
        }
        if (!GenericValidator.isBlankOrNull(form.providerLastName)
                || !GenericValidator.isBlankOrNull(form.providerFirstName)) {
            Practitioner prescriber = new Practitioner();
            prescriber.setId(CONTAINED_PRESCRIBER_ID);
            prescriber.addName().setFamily(trimToNull(form.providerLastName))
                    .addGiven(trimToNull(form.providerFirstName));
            task.addContained(prescriber);
            task.setRequester(new Reference("#" + CONTAINED_PRESCRIBER_ID));
        }
        if (organization != null && organization.getFhirUuid() != null) {
            task.getRestriction().addRecipient(new Reference("Organization/" + organization.getFhirUuidAsString()));
        }
        task.setFocus(new Reference()
                .setIdentifier(new Identifier().setSystem(testIdSystem(fhirConfig)).setValue(test.getId()))
                .setDisplay(test.getName()));
        if (!venue.isEmpty()) {
            task.setEncounter(new Reference()
                    .setIdentifier(new Identifier().setSystem(fhirConfig.getVenueIdentifierSystem()).setValue(venue)));
        }
        return task;
    }

    private static OrderPriority priorityOf(String priority) {
        if (GenericValidator.isBlankOrNull(priority)) {
            return OrderPriority.ROUTINE;
        }
        try {
            OrderPriority value = OrderPriority.valueOf(priority.trim().toUpperCase());
            return value == OrderPriority.STAT || value == OrderPriority.ASAP ? value : OrderPriority.ROUTINE;
        } catch (IllegalArgumentException e) {
            return OrderPriority.ROUTINE;
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
