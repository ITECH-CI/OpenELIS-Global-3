package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.form.SampleTbEntryForm;
import org.openelisglobal.sample.form.TbSampleTest;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn({ "springContext" })
public class TbSampleServiceImpl implements TbSampleService {

    private static final String DEFAULT_ANALYSIS_TYPE = "MANUAL";

    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private ObservationHistoryService observationHistoryService;
    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;
    @Autowired
    private PersonService personService;
    @Autowired
    private PersonAddressService personAddressService;
    @Autowired
    private AddressPartService addressPartService;
    @Autowired
    private PatientIdentityService patientIdentityService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SampleOrganizationService sampleOrganizationService;
    @Autowired
    private IStatusService statusService;

    private String sampleId;
    private String patientId;
    private String providerId;
    private Sample sample;

    @Override
    public boolean persistTbData(SampleTbEntryForm form, HttpServletRequest request) {
        boolean isOK = false;
        try {
            persistPatientData(form);
            createPatientIdentity(form, patientId);
            providerId = createPersonAndProvider(form);
            sample = persistSampleData(form);
            sampleId = sample.getId();
            persistSampleHumanData(form);

            for (int i = 0; i < form.getTbSampleTests().size(); i++) {
                TbSampleTest tbSampleTest = form.getTbSampleTests().get(i);
                tbSampleTest.setOrder(i + 1);
                tbSampleTest.setSysUserId(form.getSysUserId());
                String sampleItemId = persistSampleItemData(tbSampleTest);
                persistAnalysisData(tbSampleTest, sampleItemId);
                persistSampleItemObservations(tbSampleTest, sampleItemId);
            }

            persistSampleOrganizationData(form);
            persistSampleObservations(form);
            isOK = true;
        } catch (Exception e) {
            isOK = false;
            throw e;
        }
        return isOK;
    }

    private List<String> persistSampleObservations(SampleTbEntryForm formData) {
        List<ObservationHistory> obervations = new ArrayList<ObservationHistory>();

        // tb order reason
        if (ObjectUtils.isNotEmpty(formData.getTbOrderReason())) {
            ObservationHistory orderReason = new ObservationHistory();
            orderReason.setSampleId(sampleId);
            orderReason.setPatientId(patientId);
            orderReason.setLastupdated(DateUtil.getNowAsTimestamp());
            orderReason.setSysUserId(formData.getSysUserId());
            orderReason.setValueType(ValueType.DICTIONARY);
            orderReason.setValue(formData.getTbOrderReason());
            orderReason.setObservationHistoryTypeId(getObservationHistoryTypeId("TbOrderReason"));
            obervations.add(orderReason);
        }
        // tb diagnostic reason
        if (ObjectUtils.isNotEmpty(formData.getTbDiagnosticReason())) {
            ObservationHistory diagnosticReason = new ObservationHistory();
            diagnosticReason.setSampleId(sampleId);
            diagnosticReason.setPatientId(patientId);
            diagnosticReason.setLastupdated(DateUtil.getNowAsTimestamp());
            diagnosticReason.setSysUserId(formData.getSysUserId());
            diagnosticReason.setValueType(ValueType.DICTIONARY);
            diagnosticReason.setValue(formData.getTbDiagnosticReason());
            diagnosticReason.setObservationHistoryTypeId(getObservationHistoryTypeId("TbDiagnosticReason"));
            obervations.add(diagnosticReason);
        }
        // tb followup reason
        if (ObjectUtils.isNotEmpty(formData.getTbFollowupReason())) {
            ObservationHistory tbFollowupReason = new ObservationHistory();
            tbFollowupReason.setSampleId(sampleId);
            tbFollowupReason.setPatientId(patientId);
            tbFollowupReason.setLastupdated(DateUtil.getNowAsTimestamp());
            tbFollowupReason.setSysUserId(formData.getSysUserId());
            tbFollowupReason.setValueType(ValueType.DICTIONARY);
            tbFollowupReason.setValue(formData.getTbFollowupReason());
            tbFollowupReason.setObservationHistoryTypeId(getObservationHistoryTypeId("TbFollowupReason"));
            obervations.add(tbFollowupReason);
        }

        // tb follwup period Line 1
        if (ObjectUtils.isNotEmpty(formData.getTbFollowupPeriodLine1())) {
            ObservationHistory tbFollowupReasonPeriodLine1 = new ObservationHistory();
            tbFollowupReasonPeriodLine1.setSampleId(sampleId);
            tbFollowupReasonPeriodLine1.setPatientId(patientId);
            tbFollowupReasonPeriodLine1.setLastupdated(DateUtil.getNowAsTimestamp());
            tbFollowupReasonPeriodLine1.setSysUserId(formData.getSysUserId());
            tbFollowupReasonPeriodLine1.setValueType(ValueType.LITERAL);
            tbFollowupReasonPeriodLine1.setValue(formData.getTbFollowupPeriodLine1());
            tbFollowupReasonPeriodLine1
                    .setObservationHistoryTypeId(getObservationHistoryTypeId("TbFollowupReasonPeriodLine1"));
            obervations.add(tbFollowupReasonPeriodLine1);
        }
        // tb follwup period Line 2
        if (ObjectUtils.isNotEmpty(formData.getTbFollowupPeriodLine2())) {
            ObservationHistory tbFollowupReasonPeriodLine2 = new ObservationHistory();
            tbFollowupReasonPeriodLine2.setSampleId(sampleId);
            tbFollowupReasonPeriodLine2.setPatientId(patientId);
            tbFollowupReasonPeriodLine2.setLastupdated(DateUtil.getNowAsTimestamp());
            tbFollowupReasonPeriodLine2.setSysUserId(formData.getSysUserId());
            tbFollowupReasonPeriodLine2.setValueType(ValueType.LITERAL);
            tbFollowupReasonPeriodLine2.setValue(formData.getTbFollowupPeriodLine2());
            tbFollowupReasonPeriodLine2
                    .setObservationHistoryTypeId(getObservationHistoryTypeId("TbFollowupReasonPeriodLine2"));
            obervations.add(tbFollowupReasonPeriodLine2);
        }
        return observationHistoryService.insertAll(obervations);
    }

    private List<String> persistSampleItemObservations(TbSampleTest tbSampleTest, String sampleItemId) {
        List<ObservationHistory> obervations = new ArrayList<ObservationHistory>();
        List<ObservationHistory> sampleItemObservationsToRemove = observationHistoryService
                .getObservationHistoriesBySampleItemId(sampleItemId);
        // remove old observations
        for (int i = 0; i < sampleItemObservationsToRemove.size(); i++) {
            ObservationHistory obs = sampleItemObservationsToRemove.get(i);
            obs.setSysUserId(tbSampleTest.getSysUserId());
            observationHistoryService.delete(obs);
        }

        // tb sample aspect
        if (ObjectUtils.isNotEmpty(tbSampleTest.getTbAspect().trim())) {
            ObservationHistory tbAspect = new ObservationHistory();
            tbAspect.setSampleId(sampleId);
            tbAspect.setSampleItemId(sampleItemId);
            tbAspect.setPatientId(patientId);
            tbAspect.setLastupdated(DateUtil.getNowAsTimestamp());
            tbAspect.setSysUserId(tbSampleTest.getSysUserId());
            tbAspect.setValueType(ValueType.DICTIONARY);
            tbAspect.setValue(tbSampleTest.getTbAspect());
            tbAspect.setObservationHistoryTypeId(getObservationHistoryTypeId("TbSampleAspects"));
            obervations.add(tbAspect);
        }

        // tb Analysis Method
        if (ObjectUtils.isNotEmpty(tbSampleTest.getSelectedTbMethod().trim())) {
            ObservationHistory analysisMethod = new ObservationHistory();
            analysisMethod.setSampleId(sampleId);
            analysisMethod.setSampleItemId(sampleItemId);
            analysisMethod.setPatientId(patientId);
            analysisMethod.setLastupdated(DateUtil.getNowAsTimestamp());
            analysisMethod.setSysUserId(tbSampleTest.getSysUserId());
            analysisMethod.setValueType(ValueType.DICTIONARY);
            analysisMethod.setValue(tbSampleTest.getSelectedTbMethod());
            analysisMethod.setObservationHistoryTypeId(getObservationHistoryTypeId("TbAnalysisMethod"));
            obervations.add(analysisMethod);
        }

        return observationHistoryService.insertAll(obervations);
    }

    private Patient persistPatientData(SampleTbEntryForm formData) {
        Patient oldPatient = null;
        if (!GenericValidator.isBlankOrNull(formData.getPatientPK())) {
            oldPatient = patientService.get(formData.getPatientPK());
        }
        if (ObjectUtils.isEmpty(oldPatient) && !GenericValidator.isBlankOrNull(formData.getGuid())) {
            oldPatient = patientService.getPatientForGuid(formData.getGuid());
        }
        if (ObjectUtils.isEmpty(oldPatient) && !GenericValidator.isBlankOrNull(formData.getTbSubjectNumber())) {
            oldPatient = patientService.getByExternalId(formData.getTbSubjectNumber());
        }

        Person thisPerson = createPersonAndAddress(formData);

        if (ObjectUtils.isEmpty(oldPatient)) {
            Patient patient = new Patient();
            patient.setPerson(thisPerson);
            patient.setExternalId(formData.getTbSubjectNumber());
            patient.setBirthDateForDisplay(formData.getPatientBirthDate());
            patient.setBirthDate(DateUtil.convertStringDateToTruncatedTimestamp(formData.getPatientBirthDate()));
            patient.setGender(formData.getPatientGender());
            patient.setSysUserId(formData.getSysUserId());
            patientId = patientService.insert(patient);
            patient.setId(patientId);
            return patient;
        } else {
            // update
            oldPatient.setPerson(thisPerson);
            oldPatient.setBirthDateForDisplay(formData.getPatientBirthDate());
            oldPatient.setBirthDate(DateUtil.convertStringDateToTimestamp(formData.getPatientBirthDate() + " 00:00"));
            oldPatient.setGender(formData.getPatientGender());
            oldPatient.setSysUserId(formData.getSysUserId());
            oldPatient = patientService.update(oldPatient);
            patientId = oldPatient.getId();
            return oldPatient;
        }
    }

    // create a new Person
    private Person createPersonAndAddress(SampleTbEntryForm formData) {
        Person person = new Person();
        person.setFirstName(formData.getPatientFirstName());
        person.setLastName(formData.getPatientLastName());
        person.setLastupdatedFields();
        person.setSysUserId(formData.getSysUserId());
        String personId = personService.insert(person);
        person.setId(personId);
        createPersonAddresses(formData, personId);
        return person;
    }

    // create a new PatientIdentity
    private void createPatientIdentity(SampleTbEntryForm formData, String patientId) {
        // id type: 1
        if (ObjectUtils.isNotEmpty(formData.getTbSubjectNumber())) {
            String typeID1 = PatientIdentityTypeMap.getInstance().getIDForType("TB_PATIENT_CODE");
            PatientIdentity patientIdentity1 = patientIdentityService.getPatitentIdentityForPatientAndType(patientId,
                    typeID1);
            if (ObjectUtils.isEmpty(patientIdentity1)) {
                patientIdentity1 = new PatientIdentity();
                patientIdentity1.setPatientId(patientId);
                patientIdentity1.setIdentityData(formData.getTbSubjectNumber());
                patientIdentity1.setLastupdated(DateUtil.getNowAsTimestamp());
                patientIdentity1.setIdentityTypeId(typeID1);
                patientIdentityService.insert(patientIdentity1);
            }
        }
        // id type: 2
        if (ObjectUtils.isNotEmpty(formData.getTbSubjectNumberRes())) {
            String typeID2 = PatientIdentityTypeMap.getInstance().getIDForType("TB_PATIENT_CODE_RR");
            PatientIdentity patientIdentity2 = patientIdentityService.getPatitentIdentityForPatientAndType(patientId,
                    typeID2);
            if (ObjectUtils.isEmpty(patientIdentity2)) {
                patientIdentity2 = new PatientIdentity();
                patientIdentity2.setPatientId(patientId);
                patientIdentity2.setIdentityData(formData.getTbSubjectNumberRes());
                patientIdentity2.setLastupdated(DateUtil.getNowAsTimestamp());
                patientIdentity2.setIdentityTypeId(typeID2);
                patientIdentityService.insert(patientIdentity2);
            }
        }
    }

    private String createPersonAndProvider(SampleTbEntryForm formData) {
        Person person = new Person();
        person.setFirstName(formData.getProviderFirstName());
        person.setLastName(formData.getProviderLastName());
        person.setStreetAddress(formData.getPatientAddress());
        person.setZipCode("");
        person.setSysUserId(formData.getSysUserId());
        String personId = personService.insert(person);
        person.setId(personId);
        Provider provider = new Provider();
        provider.setPerson(person);
        provider.setLastupdated(DateUtil.getNowAsTimestamp());
        return providerService.insert(provider);
    }

    private void createPersonAddresses(SampleTbEntryForm formData, String personId) {
        // define addresses
        List<PersonAddress> existingAddresses = personAddressService.getAddressPartsByPersonId(personId);
        if (ObjectUtils.isEmpty(existingAddresses)) {
            PersonAddress patientPhone = new PersonAddress();
            patientPhone.setPersonId(personId);
            patientPhone.setAddressPartId(addressPartService.getAddresPartByName("phone").getId());
            patientPhone.setType("T");
            patientPhone.setValue(formData.getPatientPhone());
            patientPhone.setSysUserId(formData.getSysUserId());
            patientPhone.setLastupdatedFields();
            personAddressService.insert(patientPhone);
            PersonAddress patientStreetAddress = new PersonAddress();
            patientStreetAddress.setPersonId(personId);
            patientStreetAddress.setAddressPartId(addressPartService.getAddresPartByName("street").getId());
            patientStreetAddress.setType("T");
            patientStreetAddress.setValue(formData.getPatientAddress());
            patientStreetAddress.setSysUserId(formData.getSysUserId());
            patientStreetAddress.setLastupdatedFields();
            personAddressService.insert(patientStreetAddress);
        } else {
            // update adresses
            existingAddresses.forEach(address -> {
                if (address.getAddressPartId().equals(addressPartService.getAddresPartByName("phone").getId())) {
                    address.setValue(formData.getPatientPhone());
                }
                if (address.getAddressPartId().equals(addressPartService.getAddresPartByName("street").getId())) {
                    address.setValue(formData.getPatientAddress());
                }
                personAddressService.update(address);
            });
        }
    }

    private Sample persistSampleData(SampleTbEntryForm formData) {
        if (ObjectUtils.isNotEmpty(formData.getSampleId())) {
            sample = sampleService.get(formData.getSampleId());
            sample = sampleService.getSampleByAccessionNumber(formData.getLabnoForSearch());
        }
        if (ObjectUtils.isEmpty(formData.getSampleId()) || ObjectUtils.isEmpty(sample.getId())) {
            // create a new Sample
            sample = new Sample();
            sample.setAccessionNumber(formData.getLabNo());
            sample.setCollectionDateForDisplay(formData.getRequestDate());
            sample.setCollectionDate(DateUtil.convertStringDateToTruncatedTimestamp(formData.getRequestDate()));
            sample.setReceivedDateForDisplay(formData.getReceivedDate());
            sample.setReceivedDate(DateUtil.convertStringDateToSqlDate(formData.getReceivedDate()));
            sample.setEnteredDate(DateUtil.getNowAsSqlDate());
            sample.setDomain("H");
            sample.setSysUserId(formData.getSysUserId());
            sample.setLastupdated(DateUtil.getNowAsTimestamp());
            sample.setStatusId(statusService.getStatusID(OrderStatus.Entered));
            sample.setFhirUuid(UUID.randomUUID());
            sample.setPriority(OrderPriority.ROUTINE);
            sampleService.insert(sample);
            return sample;
        } else {
            // update
            sample.setCollectionDateForDisplay(formData.getRequestDate());
            sample.setCollectionDate(DateUtil.convertStringDateToTruncatedTimestamp(formData.getRequestDate()));
            sample.setReceivedDateForDisplay(formData.getReceivedDate());
            sample.setReceivedDate(DateUtil.convertStringDateToSqlDate(formData.getReceivedDate()));
            sample.setEnteredDate(DateUtil.getNowAsSqlDate());
            sample.setSysUserId(formData.getSysUserId());
            sample.setLastupdated(DateUtil.getNowAsTimestamp());
            sample.setFhirUuid(UUID.randomUUID());
            return sampleService.update(sample);
        }
    }

    private String persistSampleHumanData(SampleTbEntryForm formData) {

        if (!ObjectUtils.isEmpty(formData.getSampleId())) {
            SampleHuman sampleHuman = new SampleHuman();
            sampleHuman.setSampleId(formData.getSampleId());
            sampleHuman = sampleHumanService.getDataBySample(sampleHuman);
            if (ObjectUtils.isNotEmpty(sampleHuman)) {
                sampleHuman.setPatientId(patientId);
                sampleHuman.setLastupdated(DateUtil.getNowAsTimestamp());
                sampleHuman.setSysUserId(formData.getSysUserId());
                sampleHuman.setProviderId(providerId);
                return sampleHumanService.update(sampleHuman).getId();
            }
        }
        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSampleId(sampleId);
        sampleHuman.setPatientId(patientId);
        sampleHuman.setLastupdated(DateUtil.getNowAsTimestamp());
        sampleHuman.setSysUserId(formData.getSysUserId());
        sampleHuman.setProviderId(providerId);
        return sampleHumanService.insert(sampleHuman);
    }

    private String persistSampleItemData(TbSampleTest tbSampleTest) {
        SampleItem item = new SampleItem();
        TypeOfSample typeOfSample = typeOfSampleService.get(tbSampleTest.getTbSpecimenNature());
        if (!ObjectUtils.isEmpty(tbSampleTest.getSampleItemId())) {
            // updates
            SampleItem oldSampleItem = sampleItemService.get(tbSampleTest.getSampleItemId());
            if (ObjectUtils.isNotEmpty(oldSampleItem)) {
                oldSampleItem.setSample(sample);
                oldSampleItem.setSortOrder(tbSampleTest.getOrder() + "");
                oldSampleItem.setLastupdated(DateUtil.getNowAsTimestamp());
                oldSampleItem.setSysUserId(tbSampleTest.getSysUserId());
                oldSampleItem.setTypeOfSample(typeOfSample);
                sampleItemService.update(oldSampleItem);
                return oldSampleItem.getId();
            }
        }
        item.setSample(sample);
        item.setLastupdated(DateUtil.getNowAsTimestamp());
        item.setTypeOfSample(typeOfSample);
        item.setStatusId(statusService.getStatusID(SampleStatus.Entered));
        item.setSortOrder(tbSampleTest.getOrder() + "");
        item.setSysUserId(tbSampleTest.getSysUserId());
        return sampleItemService.insert(item);
    }

    private void persistAnalysisData(TbSampleTest tbSampleTest, String sampleItemId) {
        List<Analysis> analysisToAddItems = new ArrayList<Analysis>();
        List<Analysis> analysisToCancelItems = new ArrayList<Analysis>();

        List<String> testsToCancel = tbSampleTest.getSelectedTests();
        if (ObjectUtils.isNotEmpty(tbSampleTest.getSelectedTests())) {
            testsToCancel = tbSampleTest.getSelectedTests().stream()
                    .filter(el -> !tbSampleTest.getNewSelectedTests().contains(el)).collect(Collectors.toList());
        }

        List<String> testsToAdd = tbSampleTest.getNewSelectedTests();
        if (ObjectUtils.isNotEmpty(tbSampleTest.getSelectedTests())) {
            testsToAdd = tbSampleTest.getNewSelectedTests().stream()
                    .filter(el -> !tbSampleTest.getSelectedTests().contains(el)).collect(Collectors.toList());
        }

        if (ObjectUtils.isNotEmpty(testsToAdd))
            testsToAdd.removeAll(Collections.singleton(null));
        if (ObjectUtils.isNotEmpty(testsToCancel))
            testsToCancel.removeAll(Collections.singleton(null));

        for (String testId : testsToAdd) {
            Analysis analysis = new Analysis();
            Test test = testService.get(testId);
            SampleItem sampleItem = sampleItemService.get(sampleItemId);
            analysis.setSampleItem(sampleItem);
            analysis.setTest(test);
            analysis.setRevision("0");
            analysis.setTestSection(testSectionService.getTestSectionByName("TB"));
            analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
            analysis.setIsReportable(test.getIsReportable());
            analysis.setAnalysisType(DEFAULT_ANALYSIS_TYPE);
            analysis.setStartedDate(DateUtil.getNowAsSqlDate());
            analysis.setStatusId(statusService.getStatusID(AnalysisStatus.NotStarted));
            analysis.setSysUserId(tbSampleTest.getSysUserId());
            analysis.setFhirUuid(UUID.randomUUID());
            analysis.setSampleTypeName(typeOfSampleService.get(tbSampleTest.getTbSpecimenNature()).getDescription());
            analysisToAddItems.add(analysis);
        }

        // get status List
        Set<Integer> statusList = new HashSet<Integer>();
        statusList.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));

        if (ObjectUtils.isNotEmpty(testsToCancel)) {
            List<Integer> testsToCancelToInt = testsToCancel.stream().map(t -> Integer.parseInt(t))
                    .collect(Collectors.toList());

            SampleItem thisSampleItem = new SampleItem();
            thisSampleItem.setId(sampleItemId);
            List<Analysis> sampleItemsAnalysises = analysisService
                    .getAnalysesBySampleItemsExcludingByStatusIds(thisSampleItem, statusList);
            analysisToCancelItems = sampleItemsAnalysises.stream()
                    .filter(analysis -> testsToCancelToInt.contains(Integer.parseInt(analysis.getTest().getId())))
                    .collect(Collectors.toList());
        }

        analysisService.updateAnalysises(analysisToCancelItems, analysisToAddItems, tbSampleTest.getSysUserId());
    }

    private String persistSampleOrganizationData(SampleTbEntryForm formData) {
        SampleOrganization sampleOrganization = new SampleOrganization();
        if (ObjectUtils.isNotEmpty(formData.getSampleId())) {
            sampleOrganization = sampleOrganizationService.getDataBySample(sample);
            if (ObjectUtils.isNotEmpty(sampleOrganization)) {
                sampleOrganization.setLastupdated(DateUtil.getNowAsTimestamp());
                sampleOrganization.setSysUserId(formData.getSysUserId());
                sampleOrganization.setSample(sample);
                Organization organization = organizationService.get(formData.getReferringSiteCode());
                sampleOrganization.setOrganization(organization);
                return sampleOrganizationService.update(sampleOrganization).getId();
            }
        }
        sampleOrganization.setLastupdated(DateUtil.getNowAsTimestamp());
        sampleOrganization.setSysUserId(formData.getSysUserId());
        sampleOrganization.setSample(sample);
        sampleOrganization.setSysUserId(formData.getSysUserId());
        sampleOrganization.setOrganization(organizationService.get(formData.getReferringSiteCode()));
        return sampleOrganizationService.insert(sampleOrganization);
    }

    private String getObservationHistoryTypeId(String name) {
        ObservationHistoryType oht;
        oht = observationHistoryTypeService.getByName(name);
        if (oht != null) {
            return oht.getId();
        }
        return null;
    }

    @Override
    public SampleTbEntryForm getTBSampleFormData(String labnoForSearch) {
        SampleTbEntryForm form = new SampleTbEntryForm();
        if (ObjectUtils.isNotEmpty(labnoForSearch)) {
            form.setLabnoForSearch(labnoForSearch);
            Sample searchSample = sampleService.getSampleByAccessionNumber(labnoForSearch);
            if (ObjectUtils.isNotEmpty(searchSample)) {
                SampleOrganization sampOrg = sampleOrganizationService.getDataBySample(searchSample);
                Patient patient = sampleHumanService.getPatientForSample(searchSample);
                Provider provider = sampleHumanService.getProviderForSample(searchSample);
                PersonAddress personAddressPhone = personAddressService.getByPersonIdAndPartId(
                        patient.getPerson().getId(), addressPartService.getAddresPartByName("phone").getId());
                PersonAddress personAddressStreet = personAddressService.getByPersonIdAndPartId(
                        patient.getPerson().getId(), addressPartService.getAddresPartByName("street").getId());
                PatientIdentity patIdentity1 = patientIdentityService.getPatitentIdentityForPatientAndType(
                        patient.getId(), PatientIdentityTypeMap.getInstance().getIDForType("TB_PATIENT_CODE"));
                PatientIdentity patIdentity2 = patientIdentityService.getPatitentIdentityForPatientAndType(
                        patient.getId(), PatientIdentityTypeMap.getInstance().getIDForType("TB_PATIENT_CODE_RR"));
                List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(searchSample.getId());
                List<ObservationHistory> sampleObservations = observationHistoryService.getAll(patient, searchSample);

                Collections.sort(sampleItems, new Comparator<SampleItem>() {
                    @Override
                    public int compare(SampleItem o1, SampleItem o2) {
                        return o1.getSortOrder().compareTo(o2.getSortOrder());
                    }
                });
                List<TbSampleTest> tbSampleTests = new ArrayList<TbSampleTest>();

                for (SampleItem sampleItem : sampleItems) {
                    TbSampleTest tbSampleTest = new TbSampleTest();
                    Set<Integer> excludedAnalysisStatusList = new HashSet<>();
                    excludedAnalysisStatusList.add(Integer.parseInt(
                            SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
                    List<Analysis> analysis = analysisService.getAnalysesBySampleItemsExcludingByStatusIds(sampleItem,
                            excludedAnalysisStatusList);
                    List<ObservationHistory> sampleItemObservations = observationHistoryService
                            .getObservationHistoriesBySampleItemId(sampleItem.getId());
                    for (ObservationHistory observationHistory : sampleItemObservations) {
                        if (observationHistory.getObservationHistoryTypeId()
                                .equals(getObservationHistoryTypeId("TbSampleAspects"))
                                && ObjectUtils.isNotEmpty(observationHistory.getValue())) {
                            tbSampleTest.setTbAspect(observationHistory.getValue());
                        }
                        if (observationHistory.getObservationHistoryTypeId()
                                .equals(getObservationHistoryTypeId("TbAnalysisMethod"))
                                && ObjectUtils.isNotEmpty(observationHistory.getValue())) {
                            tbSampleTest.setSelectedTbMethod(observationHistory.getValue());
                        }

                    }

                    List<String> oldTestIds = analysis.stream().map(a -> a.getTest().getId())
                            .collect(Collectors.toList());
                    tbSampleTest.setNewSelectedTests(oldTestIds);
                    tbSampleTest.setSelectedTests(oldTestIds);
                    tbSampleTest.setTbSpecimenNature(sampleItem.getTypeOfSampleId());
                    tbSampleTest.setOrder(Integer.parseInt(sampleItem.getSortOrder()));
                    tbSampleTest.setSysUserId(sampleItem.getSysUserId());
                    tbSampleTest.setSampleItemId(sampleItem.getId());
                    tbSampleTest.setSampleId(sampleItem.getSample().getId());

                    tbSampleTests.add(tbSampleTest);

                }

                form.setTbSampleTests(tbSampleTests);
                form.setNewTbSampleTests(tbSampleTests);
                form.setSampleId(searchSample.getId());
                form.setLabNo(searchSample.getAccessionNumber());
                form.setRequestDate(searchSample.getCollectionDateForDisplay());
                form.setReceivedDate(searchSample.getReceivedDateForDisplay());
                if (ObjectUtils.isNotEmpty(sampOrg)) {
                    form.setReferringSiteId(sampOrg.getOrganization().getId());
                    form.setReferringSiteCode(sampOrg.getOrganization().getShortName());
                    form.setReferringSiteName(sampOrg.getOrganization().getName());
                }
                if (ObjectUtils.isNotEmpty(provider)) {
                    form.setProviderLastName(provider.getPerson().getLastName());
                    form.setProviderFirstName(provider.getPerson().getFirstName());
                }
                form.setPatientLastName(patient.getPerson().getLastName());
                form.setPatientFirstName(patient.getPerson().getFirstName());
                if (ObjectUtils.isNotEmpty(personAddressPhone)) {
                    form.setPatientPhone(personAddressPhone.getValue());
                }
                if (ObjectUtils.isNotEmpty(personAddressStreet)) {
                    form.setPatientAddress(personAddressStreet.getValue());
                }
                form.setPatientBirthDate(patient.getBirthDateForDisplay());
                form.setPatientGender(patient.getGender());

                if (ObjectUtils.isNotEmpty(patIdentity1))
                    form.setTbSubjectNumber(patIdentity1.getIdentityData());
                if (ObjectUtils.isNotEmpty(patIdentity2))
                    form.setTbSubjectNumberRes(patIdentity2.getIdentityData());

                // observations
                for (ObservationHistory observationHistory : sampleObservations) {
                    if (observationHistory.getObservationHistoryTypeId()
                            .equals(getObservationHistoryTypeId("TbOrderReason"))) {
                        form.setTbOrderReason(observationHistory.getValue());
                    }
                    if (observationHistory.getObservationHistoryTypeId()
                            .equals(getObservationHistoryTypeId("TbDiagnosticReason"))) {
                        form.setTbDiagnosticReason(observationHistory.getValue());
                    }
                    if (observationHistory.getObservationHistoryTypeId()
                            .equals(getObservationHistoryTypeId("TbFollowupReason"))) {
                        form.setTbFollowupReason(observationHistory.getValue());
                    }
                    if (observationHistory.getObservationHistoryTypeId()
                            .equals(getObservationHistoryTypeId("TbFollowupReasonPeriodLine1"))) {
                        form.setTbFollowupPeriodLine1(observationHistory.getValue());
                    }
                    if (observationHistory.getObservationHistoryTypeId()
                            .equals(getObservationHistoryTypeId("TbFollowupReasonPeriodLine2"))) {
                        form.setTbFollowupPeriodLine2(observationHistory.getValue());
                    }
                }

            }
        }

        return form;
    }
}
