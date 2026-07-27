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
package org.openelisglobal.dataexchange.order.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.order.ElectronicOrderSortOrderCategoryConvertor;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderPaging;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderViewForm;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pendant REST de {@link StudyElectronicOrdersController} (JSP), pour la page
 * React "Demande electronique" du menu Étude. Réutilise la même logique de
 * recherche/affichage (colonnes dénormalisées electronic_order en priorité,
 * repli FHIR uniquement pour les demandes reçues avant la migration 4a — voir
 * DESIGN_INTEROP_MODULE_CIV.md §6.1) et le même workflow de rejet.
 */
@RestController
public class RestStudyElectronicOrdersController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "searchType", "searchValue", "startDate", "endDate",
            "testIds", "statusId", "useAllInfo", "organizationId", "organizationList" };

    @Autowired
    private StatusOfSampleService statusOfSampleService;
    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private TestService testService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @InitBinder
    public void initBinder(final WebDataBinder webdataBinder) {
        webdataBinder.registerCustomEditor(ElectronicOrder.SortOrder.class,
                new ElectronicOrderSortOrderCategoryConvertor());
        webdataBinder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/rest/StudyElectronicOrders", method = RequestMethod.GET)
    public ElectronicOrderViewForm showElectronicOrders(HttpServletRequest request,
            @ModelAttribute("form") @Valid ElectronicOrderViewForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setReferralFacilitySelectionList(
                DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS));
        form.setTestSelectionList(DisplayListService.getInstance().getList(ListType.ORDERABLE_TESTS));
        form.setStatusSelectionList(DisplayListService.getInstance().getList(ListType.ELECTRONIC_ORDER_STATUSES));
        form.setOrganizationList(OrganizationTypeList.ARV_ORGS.getList());
        form.setQaEvents(DisplayListService.getInstance().getList(ListType.QA_EVENTS));

        List<ElectronicOrder> electronicOrders;
        List<ElectronicOrderDisplayItem> eOrderDisplayItems = new ArrayList<>();
        ElectronicOrderPaging paging = new ElectronicOrderPaging();
        String requestedPage = request.getParameter("page");
        if (GenericValidator.isBlankOrNull(requestedPage)) {
            if (form.getSearchType() != null) {
                electronicOrders = electronicOrderService.searchStudyElectronicOrdersCombined(form);
                eOrderDisplayItems = convertToDisplayItem(electronicOrders);
                paging.setDatabaseResults(request, form, eOrderDisplayItems);
            }
        } else {
            int requestedPageNumber = Integer.parseInt(requestedPage);
            paging.page(request, form, requestedPageNumber);
        }
        form.setSearchFinished(true);
        return form;
    }

    @PostMapping("/rest/rejectElectronicOrders")
    public ResponseEntity<Map<String, Object>> rejectElectronicOrder(@RequestBody RejectForm rejectForm) {
        Map<String, Object> out = new HashMap<>();
        if (rejectForm == null || GenericValidator.isBlankOrNull(rejectForm.externalOrderId)
                || GenericValidator.isBlankOrNull(rejectForm.qaEventId)) {
            out.put("success", false);
            out.put("message", "externalOrderId and qaEventId are required");
            return ResponseEntity.badRequest().body(out);
        }
        try {
            List<ElectronicOrder> eOrders = electronicOrderService
                    .getElectronicOrdersByExternalId(rejectForm.externalOrderId);
            ElectronicOrder eOrder = eOrders.isEmpty() ? null : eOrders.get(eOrders.size() - 1);
            if (eOrder == null) {
                out.put("success", false);
                out.put("message", "electronic order not found");
                return ResponseEntity.badRequest().body(out);
            }

            eOrder.setStatusId(
                    SpringContext.getBean(IStatusService.class).getStatusID(ExternalOrderStatus.NonConforming));
            eOrder.setRejectReasonId(rejectForm.qaEventId);
            eOrder.setRejectComment(rejectForm.qaNote);
            eOrder.setQaAuthorizer(rejectForm.qaAuthorizer);
            electronicOrderService.update(eOrder);

            // Best-effort : le store FHIR local ne contient pas systématiquement le Task
            // (electronic_order.data est la source de vérité côté OE, cf.
            // DESIGN_INTEROP_MODULE_CIV.md §6.1). Un échec ici (ex.
            // ResourceNotFoundException)
            // ne doit PAS faire échouer le rejet, déjà persisté avec succès ci-dessus.
            try {
                Optional<Task> task = fhirPersistanceService.getTaskBasedOnServiceRequest(rejectForm.externalOrderId);
                if (task.isPresent()) {
                    task.get().setStatus(TaskStatus.REJECTED);
                    fhirPersistanceService.updateFhirResourceInFhirStore(task.get());
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "rejectElectronicOrder",
                        "échec (non bloquant) de la mise à jour du Task FHIR : " + e);
            }

            out.put("success", true);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            LogEvent.logError(e);
            out.put("success", false);
            out.put("message", "reject failed");
            return ResponseEntity.internalServerError().body(out);
        }
    }

    @PostMapping("/rest/cancelElectronicOrders")
    public ResponseEntity<Map<String, Object>> cancelElectronicOrder(@RequestBody CancelForm cancelForm) {
        Map<String, Object> out = new HashMap<>();
        if (cancelForm == null || GenericValidator.isBlankOrNull(cancelForm.externalOrderId)) {
            out.put("success", false);
            out.put("message", "externalOrderId is required");
            return ResponseEntity.badRequest().body(out);
        }
        try {
            List<ElectronicOrder> eOrders = electronicOrderService
                    .getElectronicOrdersByExternalId(cancelForm.externalOrderId);
            ElectronicOrder eOrder = eOrders.isEmpty() ? null : eOrders.get(eOrders.size() - 1);
            if (eOrder == null) {
                out.put("success", false);
                out.put("message", "electronic order not found");
                return ResponseEntity.badRequest().body(out);
            }

            eOrder.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(ExternalOrderStatus.Cancelled));
            electronicOrderService.update(eOrder);

            // Best-effort : cf. commentaire équivalent dans rejectElectronicOrder
            // ci-dessus.
            try {
                Optional<Task> task = fhirPersistanceService.getTaskBasedOnServiceRequest(cancelForm.externalOrderId);
                if (task.isPresent()) {
                    task.get().setStatus(TaskStatus.CANCELLED);
                    fhirPersistanceService.updateFhirResourceInFhirStore(task.get());
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "cancelElectronicOrder",
                        "échec (non bloquant) de la mise à jour du Task FHIR : " + e);
            }

            out.put("success", true);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            LogEvent.logError(e);
            out.put("success", false);
            out.put("message", "cancel failed");
            return ResponseEntity.internalServerError().body(out);
        }
    }

    private List<ElectronicOrderDisplayItem> convertToDisplayItem(List<ElectronicOrder> electronicOrders) {
        return electronicOrders.stream().map(this::convertToDisplayItem).collect(Collectors.toList());
    }

    private ElectronicOrderDisplayItem convertToDisplayItem(ElectronicOrder electronicOrder) {
        ElectronicOrderDisplayItem displayItem = new ElectronicOrderDisplayItem();

        try {
            displayItem.setStatus(statusOfSampleService.get(electronicOrder.getStatusId()).getDefaultLocalizedName());
            displayItem.setElectronicOrderId(electronicOrder.getId());
            displayItem.setExternalOrderId(electronicOrder.getExternalId());
            displayItem.setPriority(electronicOrder.getPriority());
            displayItem.setQaEventId(electronicOrder.getRejectReasonId());
            if (electronicOrder.getOrderTimestamp() != null) {
                displayItem.setReceptionDateDisplay(DateUtil.formatDateAsText(electronicOrder.getOrderTimestamp()));
            }
            Patient patient = electronicOrder.getPatient();
            if (patient != null) {
                displayItem.setSubjectNumber(patientService.getSubjectNumber(patient));
                displayItem.setPatientNationalId(patient.getNationalId());
                displayItem.setBirthDate(patient.getBirthDateForDisplay());
                displayItem.setGender(patient.getGender());
                displayItem.setPatientUpid(patient.getUpidCode());
            } else {
                String errorMsg = "error in data collection - Patient was a null resource";
                displayItem.setWarnings(Arrays.asList(errorMsg));
            }
            // Colonnes d'affichage dénormalisées (module d'échange unifié §6.1) :
            // renseignées à la réception, elles permettent d'afficher la liste SANS
            // aucun appel au serveur FHIR (le vrai coût mesuré). On ne retombe sur les
            // lectures FHIR ci-dessous que pour les demandes ANCIENNES (colonnes nulles,
            // reçues avant la migration) ou pour les champs non dénormalisés.
            if (StringUtils.isNotBlank(electronicOrder.getTestName())) {
                displayItem.setTestName(electronicOrder.getTestName());
            }
            if (electronicOrder.getCollectionDate() != null) {
                displayItem.setCollectionDateDisplay(DateUtil.formatDateAsText(electronicOrder.getCollectionDate()));
            }
            if (StringUtils.isNotBlank(electronicOrder.getRequestingFacilityName())) {
                displayItem.setRequestingFacility(electronicOrder.getRequestingFacilityName());
            }

            Task task = fhirUtil.getFhirParser().parseResource(Task.class, electronicOrder.getData());
            displayItem.setRequestDateDisplay(DateUtil.formatDateAsText(task.getAuthoredOn()));
            if (task.hasMeta() && task.getMeta().getLastUpdated() != null) {
                displayItem.setCreationDateDisplay(DateUtil.formatDateAsText(task.getMeta().getLastUpdated()));
            }
            for (ParameterComponent parameter : task.getInput()) {
                if (parameter.getType().getCodingFirstRep().getCode().equals("CI0050005AAAAAAAAAAAAAAAAAAAAAAAAAAA")) { // VL
                    // demand date
                    if (ObjectUtils.isNotEmpty(parameter.getValue())) {
                        if (parameter.getValue() instanceof DateTimeType) {
                            DateTimeType dateValue = (DateTimeType) parameter.getValue();
                            if (ObjectUtils.isNotEmpty(dateValue))
                                displayItem.setRequestDateDisplay(DateUtil.formatDateAsText(dateValue.getValue()));
                        }
                    }
                }
            }
            if (displayItem.getRequestingFacility() == null && task.hasRestriction()
                    && task.getRestriction().hasRecipient()) {
                Organization organization = organizationService.getOrganizationByFhirId(
                        task.getRestriction().getRecipientFirstRep().getReferenceElement().getIdPart());
                if (organization != null) {
                    displayItem.setRequestingFacility(organization.getOrganizationName());
                }
            }
            // Repli : pour la plupart des demandes réelles observées, ni la colonne
            // dénormalisée ni task.restriction ne portent le site demandeur - seule
            // electronic_order.organization_id (colonne renseignée hors-Liquibase par
            // l'ancien uploader) l'a de façon fiable. Cf. le même repli déjà en place
            // dans SamplePatientEntryRestController.setupForm pour le formulaire d'édition.
            if (displayItem.getRequestingFacility() == null
                    && StringUtils.isNotBlank(electronicOrder.getOrganizationId())) {
                Organization organization = organizationService
                        .getOrganizationById(electronicOrder.getOrganizationId());
                if (organization != null) {
                    displayItem.setRequestingFacility(organization.getOrganizationName());
                }
            }

            Sample sample = sampleService.getSampleByReferringId(electronicOrder.getExternalId());
            if (sample != null) {
                displayItem.setLabNumber(sample.getAccessionNumber());
            }

            // Lecture FHIR de repli : UNIQUEMENT si un des champs de LISTE dénormalisés
            // manque encore (demande ancienne reçue avant la migration 4a).
            boolean needsFhirLookup = displayItem.getTestName() == null
                    || displayItem.getCollectionDateDisplay() == null || displayItem.getRequestingFacility() == null;
            if (needsFhirLookup) {
                IGenericClient fhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());

                ServiceRequest serviceRequest = fhirClient.read().resource(ServiceRequest.class)
                        .withId(electronicOrder.getExternalId()).execute();
                if (serviceRequest.getRequisition() != null) {
                    displayItem.setReferringLabNumber(serviceRequest.getRequisition().getValue());
                }
                org.hl7.fhir.r4.model.Patient fhirPatient = fhirClient.read() //
                        .resource(org.hl7.fhir.r4.model.Patient.class) //
                        .withId(serviceRequest.getSubject().getReferenceElement().getIdPart()) //
                        .execute();
                if (fhirPatient != null) {
                    for (Identifier identifier : fhirPatient.getIdentifier()) {
                        if (("https://openmrs.org/UPI").equals(identifier.getSystem())) {
                            displayItem.setPatientUpid(identifier.getValue());
                            break;
                        }
                        if (("http://fhir.openmrs.org/ext/patient/identifier#location")
                                .equals(identifier.getExtensionFirstRep().getUrl())) {
                            Extension extension = identifier.getExtensionFirstRep();
                            Reference locationReference = (Reference) extension.getValue();
                            String display = locationReference.getDisplay();
                            displayItem.setRequestingFacility(display);
                        }
                    }
                }

                if (displayItem.getCollectionDateDisplay() == null && serviceRequest.hasEncounter()) {
                    Encounter encounter = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath()).read()
                            .resource(Encounter.class)
                            .withId(serviceRequest.getEncounter().getReferenceElement().getIdPart()).execute();
                    if (ObjectUtils.isNotEmpty(encounter)) {
                        Period period = encounter.getPeriod();
                        if (ObjectUtils.isNotEmpty(period)) {
                            Date collectionDate = encounter.getPeriod().getStart();
                            if (ObjectUtils.isNotEmpty(collectionDate)) {
                                displayItem.setCollectionDateDisplay(DateUtil.formatDateAsText(collectionDate));
                            }
                        }
                    }
                }
                if (displayItem.getTestName() == null) {
                    Test test = null;
                    for (Coding coding : serviceRequest.getCode().getCoding()) {
                        if (coding.hasSystem()) {
                            if (coding.getSystem().equalsIgnoreCase("http://loinc.org")) {
                                List<Test> tests = testService.getActiveTestsByLoinc(coding.getCode());
                                if (tests.size() != 0) {
                                    test = tests.get(0);
                                    break;
                                }
                            }
                        }
                    }
                    if (test != null) {
                        displayItem.setTestName(test.getLocalizedTestName().getLocalizedValue());
                    }
                }
            }
        } catch (ResourceNotFoundException e) {
            String errorMsg = "error in data collection - FHIR resource not found";
            displayItem.setWarnings(Arrays.asList(errorMsg));
            LogEvent.logError(e);
        } catch (NullPointerException e) {
            String errorMsg = "error in data collection - null data";
            displayItem.setWarnings(Arrays.asList(errorMsg));
            LogEvent.logError(e);
        } catch (RuntimeException e) {
            String errorMsg = "error in data collection - unknown exception";
            displayItem.setWarnings(Arrays.asList(errorMsg));
            LogEvent.logError(e);
        }

        return displayItem;
    }

    @Override
    protected String findLocalForward(String forward) {
        return "PageNotFound";
    }

    @Override
    protected String getPageTitleKey() {
        return "eorder.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "study.eorder.browse.title";
    }

    /** Corps de la requête de rejet d'une demande électronique. */
    public static class RejectForm {
        public String externalOrderId;
        public String qaEventId;
        public String qaAuthorizer;
        public String qaNote;
    }

    /** Corps de la requête d'annulation d'une demande électronique. */
    public static class CancelForm {
        public String externalOrderId;
    }
}
