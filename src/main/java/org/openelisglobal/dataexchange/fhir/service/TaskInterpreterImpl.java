package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.HL7Exception;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.ContactComponent;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestPriority;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.ITestIdentityService;
import org.openelisglobal.common.services.TestIdentityService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.CmuIdentifiers;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.InterpreterResults;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.OrderType;
import org.openelisglobal.dataexchange.order.action.MessagePatient;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class TaskInterpreterImpl implements TaskInterpreter {

    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private FhirConfig fhirConfig;

    public enum IdentityType {
        GUID("GU"), ST_NUMBER("ST"), NATIONAL_ID("NA"), OB_NUMBER("OB"), PC_NUMBER("PC");

        private String tag;

        private IdentityType(String tag) {
            this.tag = tag;
        }

        public String getIdentifier() {
            return tag;
        }
    }

    public enum Gender {
        MALE("M"), FEMALE("F");

        private String tag;

        private Gender(String tag) {
            this.tag = tag;
        }

        public String getIdentifier() {
            return tag;
        }
    }

    public enum ServiceIdentifier {
        PANEL("P"), TEST("T");

        private String tag;

        ServiceIdentifier(String tag) {
            this.tag = tag;
        }

        public String getIdentifier() {
            return tag;
        }
    }

    @Autowired
    private TestService testService;
    @Autowired
    private PanelService panelService;

    private String labOrderNumber;
    private OrderPriority priority;
    private OrderType orderType;
    private String orderMessage;
    private Task task;
    private Patient patient;
    private ServiceRequest serviceRequest;
    private MessagePatient messagePatient;
    private Test test;
    private Panel panel;
    private List<InterpreterResults> results = new ArrayList<>();
    private List<String> unsupportedTests = new ArrayList<>();
    private List<String> unsupportedPanels = new ArrayList<>();
    private ITestIdentityService testIdentityService;

    @Override
    public List<InterpreterResults> interpret(Task incomingTask, ServiceRequest incomingServiceRequest,
            Patient incomingPatient) {

        this.task = incomingTask;
        this.serviceRequest = incomingServiceRequest;
        this.patient = incomingPatient;

        this.orderMessage = fhirContext.newJsonParser().encodeResourceToString(task);

        try {
            messagePatient = createPatientFromFHIR();
            test = createTestFromFHIR(serviceRequest);
            if (test == null) {
                panel = createPanelFromFHIR(serviceRequest);
            }
            extractOrderInformation(serviceRequest);
        } catch (HL7Exception e) {
            LogEvent.logDebug(e);
            return buildResultList(true);
        }
        return buildResultList(false);
    }

    private void extractOrderInformation(ServiceRequest serviceRequest) throws HL7Exception {
        labOrderNumber = serviceRequest.getIdentifierFirstRep().getValue();
        if (serviceRequest.hasPriority()) {
            getOrderPriorityFromIncomingOrder(serviceRequest.getPriority());
        } else {
            priority = OrderPriority.ROUTINE;
        }

        // gnr: make electronic_order.external_id longer
        if (labOrderNumber != null && labOrderNumber.length() > 60) {
            labOrderNumber = labOrderNumber.substring(labOrderNumber.length() - 60);
        }
        orderType = OrderType.REQUEST;
    }

    private static final String LOINC_SYSTEM = "http://loinc.org";

    // Codes LOINC demandés par le ServiceRequest, dans l'ordre.
    private List<String> requestedLoincCodes(ServiceRequest serviceRequest) {
        List<String> codes = new ArrayList<>();
        for (Coding coding : serviceRequest.getCode().getCoding()) {
            if (LOINC_SYSTEM.equalsIgnoreCase(coding.getSystem())) {
                if (!GenericValidator.isBlankOrNull(coding.getCode())) {
                    codes.add(coding.getCode().trim());
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "requestedLoincCodes",
                            "loinc code is missing a value in SR: " + serviceRequest.getIdElement().getIdPart());
                }
            }
        }
        return codes;
    }

    // org.openelisglobal.loinc.equivalences : « demandé=équivalent », séparés par
    // des virgules ou des points-virgules (ex. 20447-9=25836-8).
    static Map<String, String> parseLoincEquivalences(String setting) {
        Map<String, String> equivalences = new HashMap<>();
        if (GenericValidator.isBlankOrNull(setting)) {
            return equivalences;
        }
        for (String pair : setting.split("[,;]")) {
            String[] parts = pair.split("=");
            if (parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
                equivalences.put(parts[0].trim(), parts[1].trim());
            }
        }
        return equivalences;
    }

    // Code à essayer quand le code demandé ne correspond à rien (une seule fois :
    // l'équivalent d'un équivalent n'est pas suivi).
    private String equivalentOf(String loincCode) {
        return parseLoincEquivalences(fhirConfig.getLoincEquivalences()).get(loincCode);
    }

    private Test createTestFromFHIR(ServiceRequest serviceRequest) throws HL7Exception {
        LogEvent.logDebug(this.getClass().getSimpleName(), "createTestFromFHIR", "start");

        for (String loincCode : requestedLoincCodes(serviceRequest)) {
            Test found = firstTestForLoinc(loincCode);
            if (found == null) {
                String equivalent = equivalentOf(loincCode);
                if (equivalent != null) {
                    found = firstTestForLoinc(equivalent);
                    if (found != null) {
                        LogEvent.logInfo(this.getClass().getSimpleName(), "createTestFromFHIR",
                                "LOINC " + loincCode + " demandé, test trouvé sous l'équivalent " + equivalent);
                    }
                }
            }
            if (found != null) {
                return found;
            }
        }

        LogEvent.logDebug(this.getClass().getSimpleName(), "createTestFromFHIR",
                "no test found for SR: " + serviceRequest.getIdElement().getIdPart());
        return null;
    }

    private Test firstTestForLoinc(String loincCode) {
        List<Test> tests = testService.getTestsByLoincCode(loincCode);
        return tests == null || tests.isEmpty() ? null : tests.get(0);
    }

    private Panel createPanelFromFHIR(ServiceRequest serviceRequest) throws HL7Exception {
        LogEvent.logDebug(this.getClass().getSimpleName(), "createPanelFromFHIR", "start");

        List<String> codes = requestedLoincCodes(serviceRequest);
        if (!codes.isEmpty()) {
            // Comme avant : seul le premier code LOINC est cherché comme panel.
            String loincCode = codes.get(0);
            Panel found = panelService.getPanelByLoincCode(loincCode);
            if (found == null) {
                String equivalent = equivalentOf(loincCode);
                if (equivalent != null) {
                    found = panelService.getPanelByLoincCode(equivalent);
                }
            }
            return found;
        }

        LogEvent.logDebug(this.getClass().getSimpleName(), "createPanelFromFHIR",
                "no panel found for SR: " + serviceRequest.getIdElement().getIdPart());
        return null;
    }

    private MessagePatient createPatientFromFHIR() throws HL7Exception {

        MessagePatient messagePatient = new MessagePatient();

        messagePatient.setFhirUuid(patient.getIdElement().getIdPart());
        for (Identifier identifier : patient.getIdentifier()) {
            if (identifier.getType().hasCoding(fhirConfig.getOeFhirSystem() + "/genIdType", "externalId")) {
                messagePatient.setExternalId(identifier.getValue());
            }
            if ((fhirConfig.getOeFhirSystem() + "/pat_nationalId").equals(identifier.getSystem())) {
                messagePatient.setNationalId(identifier.getValue());
            }
            if ((fhirConfig.getOeFhirSystem() + "/pat_guid").equals(identifier.getSystem())) {
                messagePatient.setGuid(identifier.getValue());
            }
            if ((fhirConfig.getOeFhirSystem() + "/pat_stNumber").equals(identifier.getSystem())) {
                messagePatient.setStNumber(identifier.getValue());
            }
            if ((fhirConfig.getOeFhirSystem() + "/pat_subjectNumber").equals(identifier.getSystem())) {
                messagePatient.setSubjectNumber(identifier.getValue());
            }
        }
        // Numéro CMU reçu (système national ou anciens OID), normalisé et conforme :
        // enregistré dans le type d'identité "CMU", et dans le nationalId (qui porte
        // le numéro CMU dans les écrans) s'il est vide.
        String cmu = CmuIdentifiers.incomingCmu(patient, fhirConfig);
        if (cmu != null) {
            messagePatient.setCmuNumber(cmu);
            if (GenericValidator.isBlankOrNull(messagePatient.getNationalId())) {
                messagePatient.setNationalId(cmu);
            }
        }
        // TODO set fhirUUID of message patient
        DateType birthDate = patient.getBirthDateElement();
        if (birthDate != null) {
            String day = birthDate.getDay() == null ? DateUtil.AMBIGUOUS_DATE_SEGMENT
                    : String.format("%02d", birthDate.getDay());
            String month = birthDate.getMonth() == null ? DateUtil.AMBIGUOUS_DATE_SEGMENT
                    : String.format("%02d", birthDate.getMonth() + 1);
            String year = birthDate.getYear() == null
                    ? DateUtil.AMBIGUOUS_DATE_SEGMENT + DateUtil.AMBIGUOUS_DATE_SEGMENT
                    : String.format("%04d", birthDate.getYear());

            messagePatient.setDisplayDOB(day + "/" + month + "/" + year);
        }
        if (AdministrativeGender.MALE.equals(patient.getGender())) {
            messagePatient.setGender("M");
        } else if (AdministrativeGender.FEMALE.equals(patient.getGender())) {
            messagePatient.setGender("F");
        }

        for (Identifier identifier : patient.getIdentifier()) {
            if ("http://govmu.org".equals(identifier.getSystem())) {
                messagePatient.setNationalId(identifier.getId());
            }
            if ("passport".equals(identifier.getSystem())) {
                if (GenericValidator.isBlankOrNull(messagePatient.getNationalId())) {
                    messagePatient.setNationalId(identifier.getId());
                }
            }
        }
        messagePatient.setLastName(patient.getNameFirstRep().getFamily());
        messagePatient.setFirstName(patient.getNameFirstRep().getGivenAsSingleString());

        for (ContactPoint telecom : patient.getTelecom()) {
            if (ContactPoint.ContactPointSystem.EMAIL.equals(telecom.getSystem())) {
                messagePatient.setEmail(telecom.getValue());
            }
            if (ContactPoint.ContactPointSystem.SMS.equals(telecom.getSystem())) {
                messagePatient.setMobilePhone(telecom.getValue());
            }
            if (ContactPoint.ContactPointSystem.PHONE.equals(telecom.getSystem())) {
                messagePatient.setWorkPhone(telecom.getValue());
            }
        }
        for (Address address : patient.getAddress()) {
            for (StringType line : address.getLine()) {
                String lineValue = line.asStringValue();
                if (lineValue.startsWith("commune:")) {
                    messagePatient.setAddressCommune(lineValue.substring("commune:".length()).trim());
                } else {
                    if (GenericValidator.isBlankOrNull(messagePatient.getAddressStreet())) {
                        messagePatient.setAddressStreet(lineValue);
                    } else {
                        messagePatient.setAddressStreet(messagePatient.getAddressStreet() + ", " + lineValue);
                    }
                }
            }

            messagePatient.setAddressVillage(address.getCity());
            messagePatient.setAddressDepartment(address.getState());
            messagePatient.setAddressCountry(address.getCountry());
        }

        ContactComponent contact = patient.getContactFirstRep();
        messagePatient.setContactLastName(contact.getName().getFamily());
        messagePatient.setContactFirstName(contact.getName().getGivenAsSingleString());
        for (ContactPoint contactTelecom : contact.getTelecom()) {
            if (ContactPoint.ContactPointSystem.EMAIL.equals(contactTelecom.getSystem())) {
                messagePatient.setContactEmail(contactTelecom.getValue());
            }
            if (ContactPoint.ContactPointSystem.SMS.equals(contactTelecom.getSystem())) {
                messagePatient.setContactPhone(contactTelecom.getValue());
            }
            // if (ContactPoint.ContactPointSystem.PHONE.equals(contactTelecom.getSystem()))
            // {
            // messagePatient.setContactPhone(contactTelecom.getValue());
            // }
        }

        return messagePatient;
    }

    private List<InterpreterResults> buildResultList(boolean exceptionThrown) {
        LogEvent.logDebug(this.getClass().getSimpleName(), "buildResultList", "buildResultList: " + exceptionThrown);
        results = new ArrayList<>();

        if (exceptionThrown) {
            results.add(InterpreterResults.INTERPRET_ERROR);
        } else {
            if (orderType == OrderType.UNKNOWN) {
                results.add(InterpreterResults.UNKNOWN_REQUEST_TYPE);
            }

            if (GenericValidator.isBlankOrNull(getReferringOrderNumber())) {
                results.add(InterpreterResults.MISSING_ORDER_NUMBER);
            }

            if (orderType == OrderType.REQUEST) {
                // a GUID is no longer being sent, so no longer requiring it, it is instead
                // generated upon receiving patient
                /*
                 * if(GenericValidator.isBlankOrNull(getMessagePatient().getGuid())){
                 * results.add(InterpreterResults.MISSING_PATIENT_GUID); }
                 */

                // These are being commented out until we get confirmation on the desired
                // policy. Either the request should be rejected or the user should be required
                // to
                // fill the missing information in at the time of sample entry. Commenting these
                // out supports the latter
                if (GenericValidator.isBlankOrNull(getMessagePatient().getGender())) {
                    results.add(InterpreterResults.MISSING_PATIENT_GENDER);
                }

                if (getMessagePatient().getDisplayDOB() == null) {
                    results.add(InterpreterResults.MISSING_PATIENT_DOB);
                }

                if (getMessagePatient().getNationalId() == null && getMessagePatient().getObNumber() == null
                        && getMessagePatient().getPcNumber() == null && getMessagePatient().getStNumber() == null
                        && getMessagePatient().getExternalId() == null) {
                    results.add(InterpreterResults.MISSING_PATIENT_IDENTIFIER);
                }

                if ((test == null || !getTestIdentityService().doesActiveTestExistForLoinc(test.getLoinc()))
                        && (panel == null
                                || !getTestIdentityService().doesActivePanelExistForLoinc(panel.getLoinc()))) {
                    results.add(InterpreterResults.UNSUPPORTED_TESTS);
                    // Codes demandés, pour le motif de refus (statusReason).
                    unsupportedTests = requestedLoincCodes(serviceRequest);
                }
            }
        }

        if (results.isEmpty()) {
            results.add(InterpreterResults.OK);
        }

        return results;
    }

    @Override
    public OrderPriority getOrderPriority() {
        return priority;
    }

    @Override
    public String getReferringOrderNumber() {
        return labOrderNumber;
    }

    @Override
    public String getMessage() {
        if (task == null) {
            return null;
        }
        return (fhirContext.newJsonParser().encodeResourceToString(task));
    }

    @Override
    public MessagePatient getMessagePatient() {
        return messagePatient;
    }

    @Override
    public List<InterpreterResults> getResultStatus() {
        return results;
    }

    @Override
    public OrderType getOrderType() {
        return orderType;
    }

    @Override
    public List<String> getUnsupportedTests() {
        return unsupportedTests;
    }

    @Override
    public List<String> getUnsupportedPanels() {
        return unsupportedPanels;
    }

    private ITestIdentityService getTestIdentityService() {
        if (testIdentityService == null) {
            testIdentityService = TestIdentityService.getInstance();
        }

        return testIdentityService;
    }

    public void setTestIdentityService(ITestIdentityService testIdentityService) {
        this.testIdentityService = testIdentityService;
    }

    private void getOrderPriorityFromIncomingOrder(ServiceRequestPriority serviceRequestPriority) {
        switch (serviceRequestPriority) {
        case ROUTINE: {
            priority = OrderPriority.ROUTINE;
            break;
        }
        case ASAP: {
            priority = OrderPriority.ASAP;
            break;
        }
        case STAT: {
            priority = OrderPriority.STAT;
            break;
        }
        default:
            priority = OrderPriority.ROUTINE;
        }
    }

    @Override
    public Test getTest() {
        return test;
    }

    @Override
    public Panel getPanel() {
        return panel;
    }
}
