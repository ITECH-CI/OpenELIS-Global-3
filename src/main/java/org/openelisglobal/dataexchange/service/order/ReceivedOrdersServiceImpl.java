package org.openelisglobal.dataexchange.service.order;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ReceivedOrderRow.Etape;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.CmuNumber;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceivedOrdersServiceImpl implements ReceivedOrdersService {

    public static final String ALL_STEPS = "TOUTES";
    // Sans période, les demandes closes ne sont cherchées que sur les 30 derniers
    // jours (la liste « à traiter », elle, n'a pas de borne).
    private static final long DEFAULT_CLOSED_WINDOW_MS = 30L * 24 * 60 * 60 * 1000;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirConfig fhirConfig;

    @Override
    @Transactional(readOnly = true)
    public Page search(Criteria criteria) {
        Criteria c = criteria == null ? new Criteria() : criteria;
        String etape = GenericValidator.isBlankOrNull(c.etape) ? Etape.A_TRAITER.name() : c.etape.trim();
        boolean toProcessOnly = Etape.A_TRAITER.name().equals(etape);

        Timestamp from = parseDate(c.dateDebut, false);
        Timestamp to = parseDate(c.dateFin, true);
        if (!toProcessOnly && from == null) {
            from = new Timestamp(System.currentTimeMillis() - DEFAULT_CLOSED_WINDOW_MS);
        }
        List<String> statusIds = toProcessOnly ? openStatusIds() : new ArrayList<>();
        List<ElectronicOrder> eOrders = electronicOrderService.getRoutineElectronicOrders(statusIds, from, to);

        List<ReceivedOrderRow> rows = new ArrayList<>();
        for (List<ElectronicOrder> group : groupByTask(eOrders)) {
            ReceivedOrderRow row = toRow(group);
            if (row != null && matches(row, c, etape)) {
                rows.add(row);
            }
        }
        rows.sort(Comparator.comparingInt((ReceivedOrderRow r) -> priorityRank(r.getPriority()))
                .thenComparingLong(ReceivedOrderRow::getReceivedAtMillis));

        Page page = new Page();
        page.total = rows.size();
        page.pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, c.pageSize));
        page.page = Math.max(1, c.page);
        int start = (page.page - 1) * page.pageSize;
        page.rows = start >= rows.size() ? new ArrayList<>()
                : rows.subList(start, Math.min(rows.size(), start + page.pageSize));
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public int countToProcess() {
        Criteria criteria = new Criteria();
        criteria.etape = Etape.A_TRAITER.name();
        criteria.pageSize = 1;
        return search(criteria).total;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> detail(String electronicOrderId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        ElectronicOrder eOrder = electronicOrderService.get(electronicOrderId);
        if (eOrder == null || !eOrder.isRoutine()) {
            return detail;
        }
        Task task = parseTask(eOrder);
        List<String> notes = new ArrayList<>();
        List<Map<String, String>> clinicalInformation = new ArrayList<>();
        if (task != null) {
            for (Annotation note : task.getNote()) {
                if (note.hasText()) {
                    notes.add(note.getText());
                }
            }
            if (task.hasDescription()) {
                notes.add(task.getDescription());
            }
            for (ParameterComponent input : task.getInput()) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("label",
                        input.getType().hasText() ? input.getType().getText()
                                : input.getType().getCodingFirstRep().hasDisplay()
                                        ? input.getType().getCodingFirstRep().getDisplay()
                                        : input.getType().getCodingFirstRep().getCode());
                item.put("value",
                        input.getValue() == null ? null
                                : input.getValue().isPrimitive() ? input.getValue().primitiveValue()
                                        : input.getValue().toString());
                clinicalInformation.add(item);
            }
        }
        detail.put("notes", notes);
        detail.put("clinicalInformation", clinicalInformation);
        ReceivedOrderRow row = toRow(group(eOrder));
        detail.put("order", row);
        return detail;
    }

    // --- regroupement et lignes ---

    private List<ElectronicOrder> group(ElectronicOrder eOrder) {
        String taskId = electronicOrderService.getTaskId(eOrder);
        List<ElectronicOrder> group = taskId == null ? new ArrayList<>()
                : new ArrayList<>(electronicOrderService.getRoutineElectronicOrdersByTaskId(taskId));
        if (group.stream().noneMatch(e -> e.getId().equals(eOrder.getId()))) {
            group.add(0, eOrder);
        }
        return group;
    }

    // Une demande de la plateforme = une Task ; ses examens sont autant de demandes
    // électroniques. Ordre conservé (plus ancienne d'abord).
    List<List<ElectronicOrder>> groupByTask(List<ElectronicOrder> eOrders) {
        Map<String, List<ElectronicOrder>> groups = new LinkedHashMap<>();
        for (ElectronicOrder eOrder : eOrders) {
            String taskId = electronicOrderService.getTaskId(eOrder);
            String key = taskId != null ? "task:" + taskId : "order:" + eOrder.getId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(eOrder);
        }
        return new ArrayList<>(groups.values());
    }

    ReceivedOrderRow toRow(List<ElectronicOrder> group) {
        if (group == null || group.isEmpty()) {
            return null;
        }
        try {
            ElectronicOrder first = group.get(0);
            Task task = parseTask(first);
            ReceivedOrderRow row = new ReceivedOrderRow();
            row.setId(first.getId());
            row.setTaskId(electronicOrderService.getTaskId(first));
            row.setRequestNumber(row.getTaskId() != null ? row.getTaskId() : first.getExternalId());
            Date requested = task != null && task.getAuthoredOn() != null ? task.getAuthoredOn()
                    : first.getOrderTimestamp();
            row.setRequestDate(requested == null ? null : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(requested));
            long received = first.getOrderTimestamp() == null ? System.currentTimeMillis()
                    : first.getOrderTimestamp().getTime();
            row.setReceivedAtMillis(received);
            row.setAgeMinutes(Math.max(0, (System.currentTimeMillis() - received) / 60000));
            row.setPriority(highestPriority(group).name());
            row.setOrigine(first.getOrigine() == null ? null : first.getOrigine().name());

            Patient patient = first.getPatient();
            if (patient != null) {
                row.setPatientFirstName(patientService.getFirstName(patient));
                row.setPatientLastName(patientService.getLastName(patient));
                row.setCmuNumber(
                        CmuNumber.conformingOrNull(patientService.getCMUNumber(patient), fhirConfig.getCmuFormat()));
            }
            row.setProvisionalIdentity(row.getCmuNumber() == null);
            row.setRequestingFacility(requestingFacility(first, task));
            row.setPrescriber(prescriber(task));

            for (ElectronicOrder eOrder : group) {
                row.getExams()
                        .add(new ReceivedOrderRow.Exam(eOrder.getId(), eOrder.getExternalId(), eOrder.getTestName()));
            }
            applyStep(row, group);
            return row;
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "toRow",
                    "demande électronique " + group.get(0).getId() + " non affichable : " + e.getMessage());
            return null;
        }
    }

    // Étape de la demande : prise en charge dès qu'un bon existe pour un de ses
    // examens ; sinon à traiter, refusée, annulée ou annulée par le demandeur.
    private void applyStep(ReceivedOrderRow row, List<ElectronicOrder> group) {
        boolean requesterCancelled = group.stream().anyMatch(e -> e.getRequesterCancelledAt() != null);
        row.setCancelledByRequester(requesterCancelled);
        for (ElectronicOrder eOrder : group) {
            Sample sample = sampleService.getSampleByReferringId(eOrder.getExternalId());
            if (sample != null) {
                row.setEtape(Etape.PRISE_EN_CHARGE);
                row.setLabNumber(sample.getAccessionNumber());
                row.setReadOnly(true);
                return;
            }
        }
        Set<String> open = new HashSet<>(openStatusIds());
        String cancelled = statusService.getStatusID(ExternalOrderStatus.Cancelled);
        String refused = statusService.getStatusID(ExternalOrderStatus.NonConforming);
        if (group.stream().anyMatch(e -> open.contains(e.getStatusId()))) {
            row.setEtape(Etape.A_TRAITER);
        } else if (requesterCancelled) {
            row.setEtape(Etape.ANNULEE_DEMANDEUR);
        } else if (group.stream().anyMatch(e -> refused != null && refused.equals(e.getStatusId()))) {
            row.setEtape(Etape.REFUSEE);
        } else if (group.stream().anyMatch(e -> cancelled != null && cancelled.equals(e.getStatusId()))) {
            row.setEtape(Etape.ANNULEE);
        } else {
            row.setEtape(Etape.PRISE_EN_CHARGE);
        }
        row.setReadOnly(row.getEtape() != Etape.A_TRAITER);
    }

    private boolean matches(ReceivedOrderRow row, Criteria c, String etape) {
        if (!ALL_STEPS.equals(etape) && (row.getEtape() == null || !row.getEtape().name().equals(etape))) {
            return false;
        }
        if (!GenericValidator.isBlankOrNull(c.origine) && !c.origine.equals(row.getOrigine())) {
            return false;
        }
        if (!GenericValidator.isBlankOrNull(c.etablissement) && (row.getRequestingFacility() == null
                || !lower(row.getRequestingFacility()).contains(lower(c.etablissement.trim())))) {
            return false;
        }
        if (!GenericValidator.isBlankOrNull(c.recherche)) {
            String term = c.recherche.trim();
            String digits = CmuNumber.normalize(term);
            boolean cmu = row.getCmuNumber() != null && digits.matches("[0-9]{4,}")
                    && row.getCmuNumber().contains(digits);
            String fullName = lower((row.getPatientLastName() == null ? "" : row.getPatientLastName()) + " "
                    + (row.getPatientFirstName() == null ? "" : row.getPatientFirstName()));
            boolean name = fullName.contains(lower(term));
            boolean number = (row.getRequestNumber() != null && lower(row.getRequestNumber()).contains(lower(term)))
                    || row.getExams().stream().anyMatch(
                            e -> e.getExternalOrderId() != null && lower(e.getExternalOrderId()).contains(lower(term)));
            if (!cmu && !name && !number) {
                return false;
            }
        }
        return true;
    }

    private String requestingFacility(ElectronicOrder eOrder, Task task) {
        if (!GenericValidator.isBlankOrNull(eOrder.getRequestingFacilityName())) {
            return eOrder.getRequestingFacilityName();
        }
        if (task == null) {
            return null;
        }
        for (Reference reference : Arrays.asList(task.getRequester(), task.getLocation())) {
            if (reference != null && reference.hasReference()) {
                Organization organization = organizationService
                        .getOrganizationByFhirId(reference.getReferenceElement().getIdPart());
                if (organization != null) {
                    return organization.getOrganizationName();
                }
            }
        }
        return task.getRequester().hasDisplay()
                && ResourceType.Organization.name().equals(task.getRequester().getReferenceElement().getResourceType())
                        ? task.getRequester().getDisplay()
                        : null;
    }

    private String prescriber(Task task) {
        if (task == null || !task.hasRequester()) {
            return null;
        }
        Reference requester = task.getRequester();
        // demande saisie localement : prescripteur contenu dans la Task
        if (requester.hasReference() && requester.getReference().startsWith("#")) {
            String containedId = requester.getReference().substring(1);
            for (org.hl7.fhir.r4.model.Resource contained : task.getContained()) {
                if (contained instanceof org.hl7.fhir.r4.model.Practitioner
                        && containedId.equals(contained.getIdElement().getIdPart().replace("#", ""))) {
                    org.hl7.fhir.r4.model.HumanName name = ((org.hl7.fhir.r4.model.Practitioner) contained)
                            .getNameFirstRep();
                    String text = ((name.hasFamily() ? name.getFamily() : "") + " " + name.getGivenAsSingleString())
                            .trim();
                    return text.isEmpty() ? null : text;
                }
            }
            return null;
        }
        if (ResourceType.Practitioner.name().equals(requester.getReferenceElement().getResourceType())) {
            try {
                Provider provider = providerService
                        .getProviderByFhirId(UUID.fromString(requester.getReferenceElement().getIdPart()));
                if (provider != null && provider.getPerson() != null) {
                    return (provider.getPerson().getLastName() + " " + provider.getPerson().getFirstName()).trim();
                }
            } catch (IllegalArgumentException e) {
                // identifiant non UUID : on se rabat sur le libellé
            }
            return requester.hasDisplay() ? requester.getDisplay() : null;
        }
        return null;
    }

    private Task parseTask(ElectronicOrder eOrder) {
        if (eOrder == null || GenericValidator.isBlankOrNull(eOrder.getData())) {
            return null;
        }
        try {
            return fhirUtil.getFhirParser().parseResource(Task.class, eOrder.getData());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private List<String> openStatusIds() {
        return Arrays.asList(ExternalOrderStatus.Entered, ExternalOrderStatus.InProgress).stream()
                .map(statusService::getStatusID).filter(id -> !GenericValidator.isBlankOrNull(id))
                .collect(Collectors.toList());
    }

    private static OrderPriority highestPriority(List<ElectronicOrder> group) {
        return group.stream().map(ElectronicOrder::getPriority).filter(p -> p != null)
                .min(Comparator.comparingInt(p -> priorityRank(p.name()))).orElse(OrderPriority.ROUTINE);
    }

    static int priorityRank(String priority) {
        if (priority == null) {
            return 9;
        }
        switch (priority) {
        case "STAT":
            return 0;
        case "FUTURE_STAT":
            return 1;
        case "ASAP":
            return 2;
        case "TIMED":
            return 3;
        case "ROUTINE":
            return 4;
        default:
            return 9;
        }
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static Timestamp parseDate(String value, boolean endOfDay) {
        if (GenericValidator.isBlankOrNull(value)) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            format.setLenient(false);
            return new Timestamp(format.parse(value.trim() + (endOfDay ? " 23:59:59" : " 00:00:00")).getTime());
        } catch (java.text.ParseException e) {
            return null;
        }
    }
}
