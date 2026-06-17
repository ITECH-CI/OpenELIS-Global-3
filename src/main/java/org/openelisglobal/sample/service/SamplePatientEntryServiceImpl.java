package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.address.service.OrganizationAddressService;
import org.openelisglobal.address.valueholder.OrganizationAddress;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.notification.service.AnalysisNotificationConfigService;
import org.openelisglobal.notification.service.TestNotificationConfigService;
import org.openelisglobal.notification.valueholder.AnalysisNotificationConfig;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationMethod;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationPersonType;
import org.openelisglobal.notification.valueholder.TestNotificationConfig;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.action.bean.PatientRoutineBacterioInfo;
import org.openelisglobal.patient.action.bean.PatientTbInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.program.service.ImmunohistochemistrySampleService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SamplePatientEntryServiceImpl implements SamplePatientEntryService {

    private static final String DEFAULT_ANALYSIS_TYPE = "MANUAL";
    private final String SAMPLE_SUBJECT = "Sample Note";
    // private String currentUserId;

    @Autowired
    private OrganizationAddressService organizationAddressService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private ObservationHistoryService observationHistoryService;
    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;
    @Autowired
    private PersonService personService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private SampleRequesterService sampleRequesterService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private TestNotificationConfigService testNotificationConfigService;
    @Autowired
    private AnalysisNotificationConfigService analysisNotificationConfigService;
    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;
    @Autowired
    private ProgramSampleService programSampleService;
    @Autowired
    private PatientIdentityService patientIdentityService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;

    @Transactional
    @Override
    public void persistData(SamplePatientUpdateData updateData, PatientManagementUpdate patientUpdate,
            PatientManagementInfo patientInfo, SamplePatientEntryForm form, HttpServletRequest request) {
        boolean useInitialSampleCondition = FormFields.getInstance().useField(Field.InitialSampleCondition);
        boolean useSampleNature = FormFields.getInstance().useField(Field.SampleNature);

        persistOrganizationData(updateData);

        if (updateData.isSavePatient()) {
            patientUpdate.persistPatientData(patientInfo);
        }

        updateData.setPatientId(patientUpdate.getPatientId(form));

        persistProviderData(updateData);
        persistSampleData(updateData);
        persistRequesterData(updateData);
        if (useInitialSampleCondition) {
            persistInitialSampleConditions(updateData);
        }
        if (useSampleNature) {
            persistSampleNature(updateData);
        }

        persistObservations(updateData);

        // Persist TB data if present
        if (form.getPatientTbInfo() != null) {
            createPatientIdentity(form.getPatientTbInfo(), updateData.getPatientId());
            persistTbObservations(form.getPatientTbInfo(), updateData.getSample().getId(), updateData.getPatientId(),
                    updateData.getCurrentUserId());

        }

        // Persist Bacteriologie Classique data if present (this already covers
        // BacterioTypeExamens via the order items).
        if (form.getPatientRoutineBacterioInfo() != null) {
            persistBacterioObservations(form.getPatientRoutineBacterioInfo(), form.getPatientProperties(),
                    form.getSampleOrderItems(), updateData.getSample().getId(), updateData.getPatientId(),
                    updateData.getCurrentUserId());
        } else {
            // Safety net for orders that don't carry a PatientRoutineBacterioInfo block
            // but still selected an order type: persist BacterioTypeExamens here.
            SampleOrderItem orderItems = form.getSampleOrderItems();
            if (orderItems != null && !GenericValidator.isBlankOrNull(orderItems.getOrderType())) {
                List<ObservationHistory> orderTypeObs = new ArrayList<>();
                addObservationIfTypeExists(orderTypeObs, "BacterioTypeExamens",
                        updateData.getSample().getId(), updateData.getPatientId(),
                        updateData.getCurrentUserId(), ValueType.LITERAL, orderItems.getOrderType());
                for (ObservationHistory obs : orderTypeObs) {
                    observationHistoryService.insert(obs);
                }
            }
        }

        request.getSession().setAttribute("lastAccessionNumber", updateData.getAccessionNumber());
        request.getSession().setAttribute("lastPatientId", updateData.getPatientId());
    }

    /**
     * Liste exhaustive des types d'observation persistés par persistBacterioObservations,
     * utilisée pour purger l'état précédent avant une réinsertion (flow d'édition).
     * Doit rester synchronisée avec persistBacterioObservations() — toute nouvelle
     * observation persistée pour la bactério doit aussi figurer ici, sinon la
     * modification finit par accumuler des doublons.
     */
    private static final String[] BACTERIO_OBSERVATION_TYPE_NAMES = new String[] {
            "Pregnancy", "BacterioTypeExamens", "EPIDEMIO_WEEK", "currentHospitalization", "roomNumber",
            "CLINICAL_INFOS", "CLINICAL_INFOS_OTHER", "PREV3M_ATB", "PREV3M_ATB_LIST", "CURR_ATB",
            "CURR_ATB_LIST", "CURR_ATB_DUR", "HOSP_3M", "HOSP_3M_COUNT", "INVASIVE_GESTURE", "INDWELLING_DEVICES"
    };

    private static final String[] TB_OBSERVATION_TYPE_NAMES = new String[] {
            "TbOrderReason", "TbDiagnosticReason", "TbFollowupReason", "TbSampleAspects",
            "TbFollowupReasonPeriodLine1", "TbFollowupReasonPeriodLine2", "TbSpecimenNature", "TbAnalysisMethod"
    };

    @Transactional
    @Override
    public void replaceBacterioObservations(PatientRoutineBacterioInfo bacterioInfo, PatientManagementInfo patientInfo,
            SampleOrderItem sampleOrderItems, String sampleId, String patientId, String sysUserId) {
        // Types qui font partie du formulaire d'ordonnance (orderType IN/OUT,
        // épidémio week) : seuls la création (qui les saisit) et une modif qui
        // les ré-envoie explicitement doivent les écraser. Sinon, une modif qui
        // ne transporte plus ces champs ne doit pas les détruire — sinon on perd
        // l'orderType à chaque save de modif.
        List<String> typesToClear = new ArrayList<>();
        for (String t : BACTERIO_OBSERVATION_TYPE_NAMES) {
            boolean isOrderField = "BacterioTypeExamens".equals(t) || "EPIDEMIO_WEEK".equals(t);
            if (isOrderField) {
                boolean hasReplacement = sampleOrderItems != null
                        && ("BacterioTypeExamens".equals(t)
                                ? !GenericValidator.isBlankOrNull(sampleOrderItems.getOrderType())
                                : !GenericValidator.isBlankOrNull(sampleOrderItems.getEpidemiologicalWeek()));
                if (!hasReplacement) {
                    continue; // préserver l'obs existante
                }
            }
            typesToClear.add(t);
        }
        deleteObservationsForSampleByTypes(sampleId, typesToClear.toArray(new String[0]));
        persistBacterioObservations(bacterioInfo, patientInfo, sampleOrderItems, sampleId, patientId, sysUserId);
    }

    @Transactional
    @Override
    public void replaceTbObservations(PatientTbInfo tbInfo, String sampleId, String patientId, String sysUserId) {
        deleteObservationsForSampleByTypes(sampleId, TB_OBSERVATION_TYPE_NAMES);
        persistTbObservations(tbInfo, sampleId, patientId, sysUserId);
    }

    /**
     * Supprime toutes les ObservationHistory d'un échantillon dont le type figure
     * dans typeNames. Sans effet pour les types inconnus du référentiel ou les
     * sample-id sans observation. Volontairement défensive — appelée en édition
     * avant la réinsertion intégrale du bloc.
     */
    private void deleteObservationsForSampleByTypes(String sampleId, String[] typeNames) {
        if (sampleId == null || sampleId.isEmpty() || typeNames == null || typeNames.length == 0) {
            return;
        }
        // On construit un Sample léger pour la recherche : seul l'id compte côté
        // DAO (cf. ObservationHistoryDAOImpl.getAll : query par sampleId).
        org.openelisglobal.sample.valueholder.Sample sampleRef = new org.openelisglobal.sample.valueholder.Sample();
        sampleRef.setId(sampleId);

        for (String typeName : typeNames) {
            String typeId = getObservationHistoryTypeId(typeName);
            if (typeId == null) {
                continue;
            }
            List<ObservationHistory> existing = observationHistoryService.getAll(null, sampleRef, typeId);
            if (existing != null && !existing.isEmpty()) {
                observationHistoryService.deleteAll(existing);
            }
        }
    }

    private void persistObservations(SamplePatientUpdateData updateData) {
        deleteExistingObservations(updateData);

        for (ObservationHistory observation : updateData.getObservations()) {
            observation.setSampleId(updateData.getSample().getId());
            observation.setPatientId(updateData.getPatientId());
            observationHistoryService.insert(observation);
        }
    }

    private void deleteExistingObservations(SamplePatientUpdateData updateData) {
        if (GenericValidator.isBlankOrNull(updateData.getPatientId()) || updateData.getSample() == null
                || GenericValidator.isBlankOrNull(updateData.getSample().getId())) {
            return;
        }

        Patient patient = new Patient();
        patient.setId(updateData.getPatientId());

        List<ObservationHistory> existingObservations = observationHistoryService.getAll(patient, updateData.getSample());
        for (ObservationHistory observation : existingObservations) {
            if (!GenericValidator.isBlankOrNull(observation.getId())) {
                observationHistoryService.delete(observation.getId(), updateData.getCurrentUserId());
            }
        }
    }

    private void persistOrganizationData(SamplePatientUpdateData updateData) {
        Organization newOrganization = updateData.getNewOrganization();
        if (newOrganization != null) {
            organizationService.insert(newOrganization);
            organizationService.linkOrganizationAndType(newOrganization,
                    TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
            if (updateData.getRequesterSite() != null) {
                updateData.getRequesterSite().setRequesterId(newOrganization.getId());
            }

            for (OrganizationAddress address : updateData.getOrgAddressExtra()) {
                address.setOrganizationId(newOrganization.getId());
                organizationAddressService.insert(address);
            }
        }

        if (updateData.getCurrentOrganization() != null) {
            organizationService.update(updateData.getCurrentOrganization());
        }
        // newOrganization = updateData.getNewOrganizationDepartment();
        // if (newOrganization != null) {
        // organizationService.insert(newOrganization);
        // organizationService.linkOrganizationAndType(newOrganization,
        // TableIdService.getInstance().REFERRING_ORG_TYPE_ID);
        // if (updateData.getRequesterSite() != null) {
        // updateData.getRequesterSite().setRequesterId(newOrganization.getId());
        // }
        //
        // for (OrganizationAddress address : updateData.getOrgAddressExtra()) {
        // address.setOrganizationId(newOrganization.getId());
        // organizationAddressService.insert(address);
        // }
        // }
        //
        // if (updateData.getCurrentOrganizationDepartment() != null) {
        // organizationService.update(updateData.getCurrentOrganizationDepartment());
        // }

    }

    private void persistProviderData(SamplePatientUpdateData updateData) {
        if (updateData.getProviderPerson() != null && updateData.getProvider() != null) {

            // Only save Person if it's a new person (no ID yet)
            if (GenericValidator.isBlankOrNull(updateData.getProviderPerson().getId())) {
                personService.save(updateData.getProviderPerson());
            }
            updateData.getProvider().setPerson(updateData.getProviderPerson());

            // Only save Provider if it's a new provider (no ID yet)
            if (GenericValidator.isBlankOrNull(updateData.getProvider().getId())) {
                providerService.save(updateData.getProvider());
            }
        }
    }

    private void persistSampleData(SamplePatientUpdateData updateData) {
        String analysisRevision = ConfigurationProperties.getInstance().getPropertyValue("analysis.default.revision");

        updateData.getSample().setFhirUuid(UUID.randomUUID());
        sampleService.insertDataWithAccessionNumber(updateData.getSample());
        updateData.getSample().setPriority(updateData.getPriority());

        for (SampleAdditionalField field : updateData.getSampleFields()) {
            field.setSample(updateData.getSample());
            sampleService.saveSampleAdditionalField(field);
        }

        if (updateData.getProgramSample() != null) {
            if (updateData.getProgramQuestionnaireResponse() != null) {
                updateData.getProgramSample().setQuestionnaireResponseUuid(UUID.randomUUID());
            }
            updateData.getProgramSample().setSample(updateData.getSample());

            if (updateData.getProgramSample() instanceof PathologySample) {
                pathologySampleService.save((PathologySample) updateData.getProgramSample());
            } else if (updateData.getProgramSample() instanceof ImmunohistochemistrySample) {
                immunohistochemistrySampleService.save((ImmunohistochemistrySample) updateData.getProgramSample());
            } else {
                programSampleService.save(updateData.getProgramSample());
            }
        }

        for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
            if (GenericValidator.isBlankOrNull(sampleTestCollection.item.getFhirUuidAsString())) {
                sampleTestCollection.item.setFhirUuid(UUID.randomUUID());
            }
            // Ensure TypeOfSample is attached to the current persistence context
            if (sampleTestCollection.item.getTypeOfSample() != null) {
                String typeOfSampleId = sampleTestCollection.item.getTypeOfSample().getId();
                sampleTestCollection.item.setTypeOfSample(typeOfSampleService.get(typeOfSampleId));
            }
            String sampleId = sampleItemService.insert(sampleTestCollection.item);
            SampleItem savedItem = sampleItemService.get(sampleId);
            if (savedItem.isRejected()) {
                String rejectReasonId = savedItem.getRejectReasonId();
                String currentUserId = savedItem.getSysUserId();
                for (IdValuePair rejectReason : DisplayListService.getInstance().getList(ListType.REJECTION_REASONS)) {
                    if (rejectReasonId.equals(rejectReason.getId())) {
                        Note note = noteService.createSavableNote(savedItem, NoteType.REJECTION_REASON,
                                rejectReason.getValue(), SAMPLE_SUBJECT, currentUserId);
                        noteService.insert(note);
                        break;
                    }
                }
            }
            sampleTestCollection.analysises = new ArrayList<>();
            for (Test test : sampleTestCollection.tests) {
                test = testService.get(test.getId());

                Analysis analysis = populateAnalysis(analysisRevision, sampleTestCollection, test,
                        sampleTestCollection.testIdToUserSectionMap.get(test.getId()),
                        sampleTestCollection.testIdToUserSampleTypeMap.get(test.getId()), updateData);
                analysisService.insert(analysis);
                sampleTestCollection.analysises.add(analysis);

                if (updateData.getCustomNotificationLogic()) {
                    persistAnalysisNotificationConfigs(analysis, updateData);
                }
            }
        }

        updateData.buildSampleHuman();

        sampleHumanService.insert(updateData.getSampleHuman());

        if (updateData.getElectronicOrder() != null) {
            electronicOrderService.update(updateData.getElectronicOrder());
        }
    }

    /*
     * private void persistSampleProject() throws LIMSRuntimeException {
     * SampleProjectDAO sampleProjectDAO = new SampleProjectDAOImpl(); ProjectDAO
     * projectDAO = new ProjectDAOImpl(); Project project = new Project(); //
     * project.setId(projectId); projectDAO.getData(project);
     *
     * SampleProject sampleProject = new SampleProject();
     * sampleProject.setProject(project); sampleProject.setSample(sample);
     * sampleProject.setSysUserId(getSysUserId(request));
     * sampleProjectDAO.insertData(sampleProject); }
     */

    private void persistAnalysisNotificationConfigs(Analysis analysis, SamplePatientUpdateData updateData) {
        Optional<TestNotificationConfig> testNotificationConfig = testNotificationConfigService
                .getTestNotificationConfigForTestId(analysis.getTest().getId());
        AnalysisNotificationConfig analysisNotificationConfig = new AnalysisNotificationConfig();
        analysisNotificationConfig.setAnalysis(analysis);
        if (testNotificationConfig.isPresent()) {
            analysisNotificationConfig
                    .setDefaultPayloadTemplate(testNotificationConfig.get().getDefaultPayloadTemplate());
        }

        this.persistAnalysisNotificationConfig(analysis, updateData.getPatientEmailNotificationTestIds(),
                analysisNotificationConfig, testNotificationConfig, NotificationMethod.EMAIL,
                NotificationPersonType.PATIENT);
        this.persistAnalysisNotificationConfig(analysis, updateData.getPatientSMSNotificationTestIds(),
                analysisNotificationConfig, testNotificationConfig, NotificationMethod.SMS,
                NotificationPersonType.PATIENT);
        this.persistAnalysisNotificationConfig(analysis, updateData.getProviderEmailNotificationTestIds(),
                analysisNotificationConfig, testNotificationConfig, NotificationMethod.EMAIL,
                NotificationPersonType.PROVIDER);
        this.persistAnalysisNotificationConfig(analysis, updateData.getProviderSMSNotificationTestIds(),
                analysisNotificationConfig, testNotificationConfig, NotificationMethod.SMS,
                NotificationPersonType.PROVIDER);
        analysisNotificationConfigService.save(analysisNotificationConfig);
    }

    private void persistAnalysisNotificationConfig(Analysis analysis, List<String> testIds,
            AnalysisNotificationConfig analysisNotificationConfig,
            Optional<TestNotificationConfig> testNotificationConfig, NotificationMethod method,
            NotificationPersonType personType) {
        NotificationNature notificationNature = NotificationNature.RESULT_VALIDATION;
        NotificationConfigOption nto = analysisNotificationConfig.getOptionFor(notificationNature, method, personType);
        nto.setNotificationMethod(method);
        nto.setNotificationNature(notificationNature);
        nto.setNotificationPersonType(personType);
        if (testIds.contains(analysis.getTest().getId())) {
            nto.setActive(true);
        } else {
            nto.setActive(false);
        }

        if (testNotificationConfig.isPresent()) {
            NotificationConfigOption nto2 = testNotificationConfig.get().getOptionFor(notificationNature, method,
                    personType);
            nto.setPayloadTemplate(nto2.getPayloadTemplate());
            nto.setAdditionalContacts(new ArrayList<>());
            nto.getAdditionalContacts().addAll(nto2.getAdditionalContacts());
        }
    }

    private void persistRequesterData(SamplePatientUpdateData updateData) {
        if (updateData.getProviderPerson() != null && !org.apache.commons.validator.GenericValidator
                .isBlankOrNull(updateData.getProviderPerson().getId())) {
            SampleRequester sampleRequester = new SampleRequester();
            sampleRequester.setRequesterId(updateData.getProviderPerson().getId());
            sampleRequester.setRequesterTypeId(TableIdService.getInstance().PROVIDER_REQUESTER_TYPE_ID);
            sampleRequester.setSampleId(Long.parseLong(updateData.getSample().getId()));
            sampleRequester.setSysUserId(updateData.getCurrentUserId());
            sampleRequesterService.insert(sampleRequester);
        }

        if (updateData.getRequesterSite() != null) {
            updateData.getRequesterSite().setSampleId(Long.parseLong(updateData.getSample().getId()));
            if (updateData.getNewOrganization() != null) {
                updateData.getRequesterSite().setRequesterId(updateData.getNewOrganization().getId());
            }
            sampleRequesterService.insert(updateData.getRequesterSite());
        }

        if (updateData.getRequesterSiteDepartment() != null) {
            Organization siteDepartment = organizationService
                    .get(String.valueOf(updateData.getRequesterSiteDepartment().getRequesterId()));
            boolean orgHasType = false;
            for (OrganizationType orgType : siteDepartment.getOrganizationTypes()) {
                if (orgType.getId().equals(TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID)) {
                    orgHasType = true;
                }
            }
            if (!orgHasType) {
                organizationService.linkOrganizationAndType(siteDepartment,
                        TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID);
            }
            updateData.getRequesterSiteDepartment().setSampleId(Long.parseLong(updateData.getSample().getId()));
            // if (updateData.getNewOrganizationDepartment() != null) {
            //
            // updateData.getRequesterSite().setRequesterId(updateData.getNewOrganizationDepartment().getId());
            // }
            sampleRequesterService.insert(updateData.getRequesterSiteDepartment());
        }
    }

    private void persistInitialSampleConditions(SamplePatientUpdateData updateData) {

        for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
            List<ObservationHistory> initialConditions = sampleTestCollection.initialSampleConditionIdList;

            if (initialConditions != null) {
                for (ObservationHistory observation : initialConditions) {
                    observation.setSampleId(sampleTestCollection.item.getSample().getId());
                    observation.setSampleItemId(sampleTestCollection.item.getId());
                    observation.setPatientId(updateData.getPatientId());
                    observation.setSysUserId(updateData.getCurrentUserId());
                    observationHistoryService.insert(observation);
                }
            }
        }
    }

    private void persistSampleNature(SamplePatientUpdateData updateData) {

        for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
            ObservationHistory sampleNature = sampleTestCollection.sampleNature;

            if (sampleNature != null) {
                sampleNature.setSampleId(sampleTestCollection.item.getSample().getId());
                sampleNature.setSampleItemId(sampleTestCollection.item.getId());
                sampleNature.setPatientId(updateData.getPatientId());
                sampleNature.setSysUserId(updateData.getCurrentUserId());
                observationHistoryService.insert(sampleNature);
            }
        }
    }

    private Analysis populateAnalysis(String analysisRevision, SampleTestCollection sampleTestCollection, Test test,
            String userSelectedTestSection, String sampleTypeName, SamplePatientUpdateData updateData) {
        java.sql.Date collectionDateTime = DateUtil.convertStringDateTimeToSqlDate(sampleTestCollection.collectionDate);
        TestSection testSection = test.getTestSection();
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(userSelectedTestSection)) {
            testSection = testSectionService.get(userSelectedTestSection);
        }

        Panel panel = updateData.getSampleAddService().getPanelForTest(test);

        Analysis analysis = new Analysis();
        analysis.setTest(test);
        analysis.setPanel(panel);
        analysis.setIsReportable(test.getIsReportable());
        analysis.setAnalysisType(DEFAULT_ANALYSIS_TYPE);
        analysis.setSampleItem(sampleTestCollection.item);
        analysis.setSysUserId(sampleTestCollection.item.getSysUserId());
        analysis.setRevision(analysisRevision);
        analysis.setStartedDate(collectionDateTime == null ? DateUtil.getNowAsSqlDate() : collectionDateTime);
        if (sampleTestCollection.item.isRejected()) {
            analysis.setStatusId(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.SampleRejected));
        } else {
            analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
        }
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(sampleTypeName)) {
            analysis.setSampleTypeName(sampleTypeName);
        }
        analysis.setTestSection(testSection);
        // this will be used as an identifier for the service request as well
        analysis.setFhirUuid(UUID.randomUUID());
        return analysis;
    }

    private void persistTbObservations(PatientTbInfo tbInfo, String sampleId, String patientId, String sysUserId) {
        if (tbInfo == null) {
            return;
        }

        List<ObservationHistory> observations = new ArrayList<>();

        // TB Order Reason
        if (!GenericValidator.isBlankOrNull(tbInfo.getTbOrderReason())) {
            ObservationHistory orderReason = new ObservationHistory();
            orderReason.setSampleId(sampleId);
            orderReason.setPatientId(patientId);
            orderReason.setLastupdated(DateUtil.getNowAsTimestamp());
            orderReason.setSysUserId(sysUserId);
            orderReason.setValueType(ValueType.DICTIONARY);
            orderReason.setValue(tbInfo.getTbOrderReason());
            orderReason.setObservationHistoryTypeId(getObservationHistoryTypeId("TbOrderReason"));
            observations.add(orderReason);
        }

        // TB Diagnostic Reason
        if (!GenericValidator.isBlankOrNull(tbInfo.getTbDiagnosticReason())) {
            ObservationHistory diagnosticReason = new ObservationHistory();
            diagnosticReason.setSampleId(sampleId);
            diagnosticReason.setPatientId(patientId);
            diagnosticReason.setLastupdated(DateUtil.getNowAsTimestamp());
            diagnosticReason.setSysUserId(sysUserId);
            diagnosticReason.setValueType(ValueType.DICTIONARY);
            diagnosticReason.setValue(tbInfo.getTbDiagnosticReason());
            diagnosticReason.setObservationHistoryTypeId(getObservationHistoryTypeId("TbDiagnosticReason"));
            observations.add(diagnosticReason);
        }

        // TB Followup Reason
        if (!GenericValidator.isBlankOrNull(tbInfo.getTbFollowupReason())) {
            ObservationHistory followupReason = new ObservationHistory();
            followupReason.setSampleId(sampleId);
            followupReason.setPatientId(patientId);
            followupReason.setLastupdated(DateUtil.getNowAsTimestamp());
            followupReason.setSysUserId(sysUserId);
            followupReason.setValueType(ValueType.DICTIONARY);
            followupReason.setValue(tbInfo.getTbFollowupReason());
            followupReason.setObservationHistoryTypeId(getObservationHistoryTypeId("TbFollowupReason"));
            observations.add(followupReason);
        }

        // TB Aspect
        if (!GenericValidator.isBlankOrNull(tbInfo.getTbAspect())) {
            ObservationHistory aspect = new ObservationHistory();
            aspect.setSampleId(sampleId);
            aspect.setPatientId(patientId);
            aspect.setLastupdated(DateUtil.getNowAsTimestamp());
            aspect.setSysUserId(sysUserId);
            aspect.setValueType(ValueType.DICTIONARY);
            aspect.setValue(tbInfo.getTbAspect());
            aspect.setObservationHistoryTypeId(getObservationHistoryTypeId("TbSampleAspects"));
            observations.add(aspect);
        }

        // TB Followup Period Line 1
        if (!GenericValidator.isBlankOrNull(tbInfo.getTbFollowupPeriodLine1())) {
            ObservationHistory periodLine1 = new ObservationHistory();
            periodLine1.setSampleId(sampleId);
            periodLine1.setPatientId(patientId);
            periodLine1.setLastupdated(DateUtil.getNowAsTimestamp());
            periodLine1.setSysUserId(sysUserId);
            periodLine1.setValueType(ValueType.LITERAL);
            periodLine1.setValue(tbInfo.getTbFollowupPeriodLine1());
            periodLine1.setObservationHistoryTypeId(getObservationHistoryTypeId("TbFollowupReasonPeriodLine1"));
            observations.add(periodLine1);
        }

        // TB Followup Period Line 2
        if (!GenericValidator.isBlankOrNull(tbInfo.getTbFollowupPeriodLine2())) {
            ObservationHistory periodLine2 = new ObservationHistory();
            periodLine2.setSampleId(sampleId);
            periodLine2.setPatientId(patientId);
            periodLine2.setLastupdated(DateUtil.getNowAsTimestamp());
            periodLine2.setSysUserId(sysUserId);
            periodLine2.setValueType(ValueType.LITERAL);
            periodLine2.setValue(tbInfo.getTbFollowupPeriodLine2());
            periodLine2.setObservationHistoryTypeId(getObservationHistoryTypeId("TbFollowupReasonPeriodLine2"));
            observations.add(periodLine2);
        }

        // TB Specimen Nature
        if (!GenericValidator.isBlankOrNull(tbInfo.getTbSpecimenNature())) {
            ObservationHistory specimenNature = new ObservationHistory();
            specimenNature.setSampleId(sampleId);
            specimenNature.setPatientId(patientId);
            specimenNature.setLastupdated(DateUtil.getNowAsTimestamp());
            specimenNature.setSysUserId(sysUserId);
            specimenNature.setValueType(ValueType.DICTIONARY);
            specimenNature.setValue(tbInfo.getTbSpecimenNature());
            specimenNature.setObservationHistoryTypeId(getObservationHistoryTypeId("TbSpecimenNature"));
            observations.add(specimenNature);
        }

        // TB Analysis Method
        if (!GenericValidator.isBlankOrNull(tbInfo.getSelectedTbMethod())) {
            ObservationHistory analysisMethod = new ObservationHistory();
            analysisMethod.setSampleId(sampleId);
            analysisMethod.setPatientId(patientId);
            analysisMethod.setLastupdated(DateUtil.getNowAsTimestamp());
            analysisMethod.setSysUserId(sysUserId);
            analysisMethod.setValueType(ValueType.DICTIONARY);
            analysisMethod.setValue(tbInfo.getSelectedTbMethod());
            analysisMethod.setObservationHistoryTypeId(getObservationHistoryTypeId("TbAnalysisMethod"));
            observations.add(analysisMethod);
        }
        // Persist all observations
        for (ObservationHistory observation : observations) {
            observationHistoryService.insert(observation);
        }
    }

    private void persistBacterioObservations(PatientRoutineBacterioInfo bacterioInfo, PatientManagementInfo patientInfo,
            SampleOrderItem sampleOrderItems, String sampleId, String patientId, String sysUserId) {
        if (bacterioInfo == null) {
            return;
        }

        List<ObservationHistory> observations = new ArrayList<>();

        // Pregnant status from patient properties (Boolean: true/false)
        if (patientInfo != null && !GenericValidator.isBlankOrNull(patientInfo.getPregnant())) {
            addObservationIfTypeExists(observations, "Pregnancy", sampleId, patientId, sysUserId, ValueType.LITERAL,
                    patientInfo.getPregnant());
        }

        // Order Type from sample order items
        if (sampleOrderItems != null && !GenericValidator.isBlankOrNull(sampleOrderItems.getOrderType())) {
            addObservationIfTypeExists(observations, "BacterioTypeExamens", sampleId, patientId, sysUserId,
                    ValueType.LITERAL, sampleOrderItems.getOrderType());
        }

        // Epidemiological Week from sample order items
        if (sampleOrderItems != null && !GenericValidator.isBlankOrNull(sampleOrderItems.getEpidemiologicalWeek())) {
            addObservationIfTypeExists(observations, "EPIDEMIO_WEEK", sampleId, patientId, sysUserId, ValueType.LITERAL,
                    sampleOrderItems.getEpidemiologicalWeek());
        }

        // Current Hospitalization (Boolean: true/false)
        if (bacterioInfo.getCurrentHospitalization() != null) {
            addObservationIfTypeExists(observations, "currentHospitalization", sampleId, patientId, sysUserId,
                    ValueType.LITERAL, bacterioInfo.getCurrentHospitalization().toString());
        }

        // Room Number
        if (!GenericValidator.isBlankOrNull(bacterioInfo.getRoomNumber())) {
            addObservationIfTypeExists(observations, "roomNumber", sampleId, patientId, sysUserId, ValueType.LITERAL,
                    bacterioInfo.getRoomNumber());
        }

        // Clinical Informations (list of dictionary IDs)
        if (bacterioInfo.getClinicalInformations() != null && !bacterioInfo.getClinicalInformations().isEmpty()) {
            for (Integer clinicalInfoId : bacterioInfo.getClinicalInformations()) {
                addObservationIfTypeExists(observations, "CLINICAL_INFOS", sampleId, patientId, sysUserId,
                        ValueType.DICTIONARY, clinicalInfoId.toString());
            }
        }

        // Clinical Information Other (free text)
        if (!GenericValidator.isBlankOrNull(bacterioInfo.getClinicalInformationOther())) {
            addObservationIfTypeExists(observations, "CLINICAL_INFOS_OTHER", sampleId, patientId, sysUserId,
                    ValueType.LITERAL, bacterioInfo.getClinicalInformationOther());
        }

        // Recent Antibiotherapy (Boolean: true/false)
        if (bacterioInfo.getRecentAntibiotherapy() != null) {
            addObservationIfTypeExists(observations, "PREV3M_ATB", sampleId, patientId, sysUserId, ValueType.LITERAL,
                    bacterioInfo.getRecentAntibiotherapy().toString());
        }

        // Recent Antibiotherapy List
        if (bacterioInfo.getRecentAntibiotherapyList() != null
                && !bacterioInfo.getRecentAntibiotherapyList().isEmpty()) {
            for (Integer atbId : bacterioInfo.getRecentAntibiotherapyList()) {
                addObservationIfTypeExists(observations, "PREV3M_ATB_LIST", sampleId, patientId, sysUserId,
                        ValueType.DICTIONARY, atbId.toString());
            }
        }

        // Current Antibiotherapy (Boolean: true/false)
        if (bacterioInfo.getCurrentAntibiotherapy() != null) {
            addObservationIfTypeExists(observations, "CURR_ATB", sampleId, patientId, sysUserId, ValueType.LITERAL,
                    bacterioInfo.getCurrentAntibiotherapy().toString());
        }

        // Current Antibiotherapy List
        if (bacterioInfo.getCurrentAntibiotherapyList() != null
                && !bacterioInfo.getCurrentAntibiotherapyList().isEmpty()) {
            for (Integer atbId : bacterioInfo.getCurrentAntibiotherapyList()) {
                addObservationIfTypeExists(observations, "CURR_ATB_LIST", sampleId, patientId, sysUserId,
                        ValueType.DICTIONARY, atbId.toString());
            }
        }

        // Current Antibiotherapy Duration
        if (bacterioInfo.getCurrentAntibiotherapyDuration() != null) {
            addObservationIfTypeExists(observations, "CURR_ATB_DUR", sampleId, patientId, sysUserId, ValueType.LITERAL,
                    bacterioInfo.getCurrentAntibiotherapyDuration().toString());
        }

        // Recent Hospitalization (Boolean: true/false)
        if (bacterioInfo.getRecentHospitalization() != null) {
            addObservationIfTypeExists(observations, "HOSP_3M", sampleId, patientId, sysUserId, ValueType.LITERAL,
                    bacterioInfo.getRecentHospitalization().toString());
        }

        // Recent Hospitalization Count
        if (bacterioInfo.getRecentHospitalizationCount() != null) {
            addObservationIfTypeExists(observations, "HOSP_3M_COUNT", sampleId, patientId, sysUserId, ValueType.LITERAL,
                    bacterioInfo.getRecentHospitalizationCount().toString());
        }

        // Recent Invasive Gestures
        if (bacterioInfo.getRecentInvasiveGestures() != null && !bacterioInfo.getRecentInvasiveGestures().isEmpty()) {
            for (Integer gestureId : bacterioInfo.getRecentInvasiveGestures()) {
                addObservationIfTypeExists(observations, "INVASIVE_GESTURE", sampleId, patientId, sysUserId,
                        ValueType.DICTIONARY, gestureId.toString());
            }
        }

        // Indwelling Device
        if (bacterioInfo.getIndwellingDevice() != null && !bacterioInfo.getIndwellingDevice().isEmpty()) {
            for (Integer deviceId : bacterioInfo.getIndwellingDevice()) {
                addObservationIfTypeExists(observations, "INDWELLING_DEVICES", sampleId, patientId, sysUserId,
                        ValueType.DICTIONARY, deviceId.toString());
            }
        }

        // Persist all observations
        for (ObservationHistory observation : observations) {
            observationHistoryService.insert(observation);
        }
    }

    private String getObservationHistoryTypeId(String name) {
        ObservationHistoryType oht = observationHistoryTypeService.getByName(name);
        if (oht != null) {
            return oht.getId();
        }
        LogEvent.logWarn(this.getClass().getSimpleName(), "getObservationHistoryTypeId",
                "ObservationHistoryType not found for name: " + name);
        return null;
    }

    private void addObservationIfTypeExists(List<ObservationHistory> observations, String typeName, String sampleId,
            String patientId, String sysUserId, ValueType valueType, String value) {
        String typeId = getObservationHistoryTypeId(typeName);
        if (typeId != null) {
            ObservationHistory observation = new ObservationHistory();
            observation.setSampleId(sampleId);
            observation.setPatientId(patientId);
            observation.setLastupdated(DateUtil.getNowAsTimestamp());
            observation.setSysUserId(sysUserId);
            observation.setValueType(valueType);
            observation.setValue(value);
            observation.setObservationHistoryTypeId(typeId);
            observations.add(observation);
        }
    }

    private String createPatientIdentity(PatientTbInfo tbInfo, String patientId) {
        String typeID = PatientIdentityTypeMap.getInstance().getIDForType("SUBJECT");
        PatientIdentity patientIdentity = patientIdentityService.getPatitentIdentityForPatientAndType(patientId,
                typeID);
        if (ObjectUtils.isEmpty(patientIdentity)) {
            patientIdentity = new PatientIdentity();
            patientIdentity.setPatientId(patientId);
            patientIdentity.setIdentityData(tbInfo.getTbSubjectNumber());
            patientIdentity.setLastupdated(DateUtil.getNowAsTimestamp());
            patientIdentity.setIdentityTypeId(typeID);
            return patientIdentityService.insert(patientIdentity);
        } else {
            return patientIdentity.getId();
        }
    }
}
