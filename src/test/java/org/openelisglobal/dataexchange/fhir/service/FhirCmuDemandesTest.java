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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ITestIdentityService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.TaskWorker.TaskResult;
import org.openelisglobal.dataexchange.order.action.IOrderExistanceChecker;
import org.openelisglobal.dataexchange.order.action.IOrderExistanceChecker.CheckResult;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.InterpreterResults;
import org.openelisglobal.dataexchange.order.action.IOrderPersister;
import org.openelisglobal.dataexchange.order.action.MessagePatient;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Version 3.3.1.4 : numéro CMU unique sous le système national, numéro CMU des
 * patients reçus, équivalences LOINC et motif de refus des demandes
 * électroniques, date de prélèvement des Observations.
 *
 * <p>
 * Tests unitaires (collaborateurs simulés), sur le modèle de
 * {@link FhirBdmsPiloteTest}. Patients fictifs uniquement, numéros CMU
 * commençant par 9992611.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FhirCmuDemandesTest {

    private static final String OE_SYSTEM = "http://openelis-global.org";
    private static final String CMU_SYSTEM = "https://hie.ci/patient/cmu";
    private static final String CMU_COMPAT = "urn:oid:1.3.6.1.4.1.53864.1.3";
    private static final String CMU_OLD = "urn:oid:1.3.6.1.4.1.53864.1.2";
    private static final String DEMO_CMU = "9992611000001";

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private PatientService patientService;
    @Mock
    private IStatusService statusService;
    @Mock
    private org.openelisglobal.samplehuman.service.SampleHumanService sampleHumanService;
    @Mock
    private org.openelisglobal.result.service.ResultService resultService;

    @InjectMocks
    private FhirTransformServiceImpl fhirTransformService;

    private TableIdService previousTableIdService;
    private CapturingAppender appender;
    private static Object previousSpringFactory;

    // DateUtil (date de la demande reçue) lit sa configuration par SpringContext :
    // fabrique simulée le temps de la classe de test.
    @BeforeClass
    public static void installSpringFactory() throws Exception {
        Field factory = SpringContext.class.getDeclaredField("factory");
        factory.setAccessible(true);
        previousSpringFactory = factory.get(null);
        DefaultConfigurationProperties properties = mock(DefaultConfigurationProperties.class,
                invocation -> String.class.equals(invocation.getMethod().getReturnType()) ? "X"
                        : Answers.RETURNS_DEFAULTS.answer(invocation));
        AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(properties);
        factory.set(null, beanFactory);
    }

    @AfterClass
    public static void restoreSpringFactory() throws Exception {
        Field factory = SpringContext.class.getDeclaredField("factory");
        factory.setAccessible(true);
        factory.set(null, previousSpringFactory);
    }

    @Before
    public void setUp() throws Exception {
        configureFhir(fhirConfig);
        previousTableIdService = swapTableIdService(mock(TableIdService.class));
        appender = CapturingAppender.attach();
    }

    @After
    public void tearDown() throws Exception {
        swapTableIdService(previousTableIdService);
        appender.detach();
    }

    private static void configureFhir(FhirConfig config) {
        when(config.getOeFhirSystem()).thenReturn(OE_SYSTEM);
        when(config.getCmuIdentifierSystem()).thenReturn(CMU_SYSTEM);
        when(config.getCmuIdentifierSystemCompat()).thenReturn(CMU_COMPAT);
        when(config.getCmuFormat()).thenReturn("");
        when(config.getCmuAcceptedSystems()).thenReturn(new String[] { CMU_SYSTEM, CMU_COMPAT, CMU_OLD });
        when(config.getLoincEquivalences()).thenReturn("");
    }

    private static TableIdService swapTableIdService(TableIdService replacement) throws Exception {
        Field instance = TableIdService.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        TableIdService previous = (TableIdService) instance.get(null);
        instance.set(null, replacement);
        return previous;
    }

    // ---------------------------------------------------------------- sortie

    private Patient demoPatient(String cmuNumber) {
        Patient patient = new Patient();
        patient.setId("101");
        patient.setFhirUuid(UUID.randomUUID());
        when(patientService.get("101")).thenReturn(patient);
        when(patientService.getCMUNumber(patient)).thenReturn(cmuNumber);
        when(patientService.getNationalId(patient)).thenReturn("");
        return patient;
    }

    private static List<String> valuesFor(org.hl7.fhir.r4.model.Patient fhirPatient, String system) {
        return fhirPatient.getIdentifier().stream().filter(i -> system.equals(i.getSystem())).map(Identifier::getValue)
                .collect(Collectors.toList());
    }

    @Test
    public void patient_conformingCmu_shouldBeEmittedUnderNationalAndCompatSystems() {
        demoPatient(DEMO_CMU);

        org.hl7.fhir.r4.model.Patient fhirPatient = fhirTransformService.transformToFhirPatient("101");

        assertEquals(Arrays.asList(DEMO_CMU), valuesFor(fhirPatient, CMU_SYSTEM));
        assertEquals(Arrays.asList(DEMO_CMU), valuesFor(fhirPatient, CMU_COMPAT));
        Identifier national = fhirPatient.getIdentifier().stream().filter(i -> CMU_SYSTEM.equals(i.getSystem()))
                .findFirst().get();
        assertEquals(Identifier.IdentifierUse.OFFICIAL, national.getUse());
    }

    @Test
    public void patient_cmuWithSpacesOrDashes_shouldBeNormalizedThenEmitted() {
        demoPatient("999 2611-000.001");

        org.hl7.fhir.r4.model.Patient fhirPatient = fhirTransformService.transformToFhirPatient("101");

        assertEquals(Arrays.asList(DEMO_CMU), valuesFor(fhirPatient, CMU_SYSTEM));
        assertEquals(Arrays.asList(DEMO_CMU), valuesFor(fhirPatient, CMU_COMPAT));
    }

    @Test
    public void patient_twelveDigitCmu_shouldNotBeEmitted_andWarningShouldNotContainValue() {
        demoPatient("999261100000");

        org.hl7.fhir.r4.model.Patient fhirPatient = fhirTransformService.transformToFhirPatient("101");

        assertTrue(valuesFor(fhirPatient, CMU_SYSTEM).isEmpty());
        assertTrue(valuesFor(fhirPatient, CMU_COMPAT).isEmpty());
        List<String> warnings = appender.messagesContaining("CMU non conforme");
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("patient id=101"));
        assertFalse(warnings.get(0).contains("999261100000"));
    }

    @Test
    public void patient_cmuWithLetters_shouldNotBeEmitted() {
        demoPatient("999261100000A");

        org.hl7.fhir.r4.model.Patient fhirPatient = fhirTransformService.transformToFhirPatient("101");

        assertTrue(valuesFor(fhirPatient, CMU_SYSTEM).isEmpty());
        assertTrue(valuesFor(fhirPatient, CMU_COMPAT).isEmpty());
        assertFalse(appender.allMessages().contains("999261100000A"));
    }

    @Test
    public void patient_nonConformingCmu_shouldKeepPatNationalId() {
        Patient patient = demoPatient("999261100000");
        when(patientService.getNationalId(patient)).thenReturn("999261100000");

        org.hl7.fhir.r4.model.Patient fhirPatient = fhirTransformService.transformToFhirPatient("101");

        assertEquals(Arrays.asList("999261100000"), valuesFor(fhirPatient, OE_SYSTEM + "/pat_nationalId"));
        assertTrue(valuesFor(fhirPatient, CMU_SYSTEM).isEmpty());
    }

    @Test
    public void patient_emptyCompatSetting_shouldEmitNationalSystemOnly() {
        when(fhirConfig.getCmuIdentifierSystemCompat()).thenReturn("");
        demoPatient(DEMO_CMU);

        org.hl7.fhir.r4.model.Patient fhirPatient = fhirTransformService.transformToFhirPatient("101");

        assertEquals(Arrays.asList(DEMO_CMU), valuesFor(fhirPatient, CMU_SYSTEM));
        assertTrue(valuesFor(fhirPatient, CMU_COMPAT).isEmpty());
    }

    // ---------------------------------------------------------------- entrée

    private static org.hl7.fhir.r4.model.Patient incomingPatient(String system, String value) {
        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
        fhirPatient.setId(UUID.randomUUID().toString());
        fhirPatient.addIdentifier().setSystem(system).setValue(value);
        fhirPatient.setGender(AdministrativeGender.FEMALE);
        fhirPatient.setBirthDateElement(new DateType("1990-05-17"));
        fhirPatient.addName().setFamily("DEMO").addGiven("Patiente");
        return fhirPatient;
    }

    // Sans date de naissance : son formatage d'affichage passe par MessageUtil
    // (contexte Spring), hors du sujet de ces tests.
    private static org.hl7.fhir.r4.model.Patient searchedPatient(String system, String value) {
        org.hl7.fhir.r4.model.Patient fhirPatient = incomingPatient(system, value);
        fhirPatient.setBirthDateElement(null);
        return fhirPatient;
    }

    @Test
    public void patientSearchResults_threeAcceptedSystems_shouldGiveCmuAsNationalId() {
        for (String system : Arrays.asList(CMU_SYSTEM, CMU_COMPAT, CMU_OLD)) {
            PatientSearchResults results = fhirTransformService
                    .transformToOpenElisPatientSearchResults(searchedPatient(system, "999 2611 000 001"));
            assertEquals(system, DEMO_CMU, results.getNationalId());
        }
    }

    @Test
    public void patientSearchResults_nonConformingCmu_shouldBeIgnored() {
        PatientSearchResults results = fhirTransformService
                .transformToOpenElisPatientSearchResults(searchedPatient(CMU_SYSTEM, "99926110000"));
        assertNull(results.getNationalId());
    }

    private TaskInterpreterImpl newInterpreter(TestService testService, ITestIdentityService testIdentityService)
            throws Exception {
        TaskInterpreterImpl interpreter = new TaskInterpreterImpl();
        setField(interpreter, "fhirContext", mock(ca.uhn.fhir.context.FhirContext.class, Answers.RETURNS_DEEP_STUBS));
        setField(interpreter, "fhirConfig", fhirConfig);
        setField(interpreter, "testService", testService);
        setField(interpreter, "panelService", mock(PanelService.class));
        setField(interpreter, "testIdentityService", testIdentityService);
        return interpreter;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static ServiceRequest demoServiceRequest(String loinc) {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId("DEMO-SR-1");
        serviceRequest.addIdentifier().setValue("DEMO-DEMANDE-1");
        serviceRequest.setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", loinc, null)));
        return serviceRequest;
    }

    private static org.openelisglobal.test.valueholder.Test labTest(String loinc) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("501");
        test.setLoinc(loinc);
        return test;
    }

    @Test
    public void taskInterpreter_threeAcceptedSystems_shouldGiveCmuNumber() throws Exception {
        TestService testService = mock(TestService.class);
        when(testService.getTestsByLoincCode(anyString())).thenReturn(Collections.emptyList());
        for (String system : Arrays.asList(CMU_SYSTEM, CMU_COMPAT, CMU_OLD)) {
            TaskInterpreterImpl interpreter = newInterpreter(testService, mock(ITestIdentityService.class));
            interpreter.interpret(new Task(), demoServiceRequest("25836-8"), incomingPatient(system, DEMO_CMU));
            MessagePatient messagePatient = interpreter.getMessagePatient();
            assertEquals(system, DEMO_CMU, messagePatient.getCmuNumber());
            assertEquals(system, DEMO_CMU, messagePatient.getNationalId());
        }
    }

    @Test
    public void taskInterpreter_nonConformingCmu_shouldNotBeKept() throws Exception {
        TestService testService = mock(TestService.class);
        when(testService.getTestsByLoincCode(anyString())).thenReturn(Collections.emptyList());
        TaskInterpreterImpl interpreter = newInterpreter(testService, mock(ITestIdentityService.class));
        interpreter.interpret(new Task(), demoServiceRequest("25836-8"), incomingPatient(CMU_SYSTEM, "DEMO-123"));
        assertNull(interpreter.getMessagePatient().getCmuNumber());
    }

    private TestService labWithTestCodedAs(String loinc, ITestIdentityService testIdentityService) {
        TestService testService = mock(TestService.class);
        when(testService.getTestsByLoincCode(anyString())).thenReturn(Collections.emptyList());
        when(testService.getTestsByLoincCode(loinc)).thenReturn(new ArrayList<>(Arrays.asList(labTest(loinc))));
        when(testIdentityService.doesActiveTestExistForLoinc(loinc)).thenReturn(true);
        return testService;
    }

    @Test
    public void taskInterpreter_loincEquivalence_shouldAcceptRequest() throws Exception {
        when(fhirConfig.getLoincEquivalences()).thenReturn("20447-9=25836-8");
        ITestIdentityService testIdentityService = mock(ITestIdentityService.class);
        TaskInterpreterImpl interpreter = newInterpreter(labWithTestCodedAs("25836-8", testIdentityService),
                testIdentityService);

        List<InterpreterResults> results = interpreter.interpret(new Task(), demoServiceRequest("20447-9"),
                incomingPatient(CMU_SYSTEM, DEMO_CMU));

        assertEquals(Arrays.asList(InterpreterResults.OK), results);
        assertEquals("25836-8", interpreter.getTest().getLoinc());
    }

    private TaskWorker worker(TaskInterpreterImpl interpreter) {
        TaskWorker worker = new TaskWorker(new Task(), "{}", demoServiceRequest("20447-9"),
                incomingPatient(CMU_SYSTEM, DEMO_CMU));
        IOrderExistanceChecker checker = mock(IOrderExistanceChecker.class);
        when(checker.check(any())).thenReturn(CheckResult.NOT_FOUND);
        worker.setInterpreter(interpreter);
        worker.setExistanceChecker(checker);
        worker.setPersister(mock(IOrderPersister.class));
        worker.setStatusService(mock(IStatusService.class));
        return worker;
    }

    @Test
    public void taskWorker_loincEquivalence_shouldBeAcceptedWithoutReason() throws Exception {
        when(fhirConfig.getLoincEquivalences()).thenReturn("20447-9=25836-8");
        ITestIdentityService testIdentityService = mock(ITestIdentityService.class);
        TaskWorker worker = worker(
                newInterpreter(labWithTestCodedAs("25836-8", testIdentityService), testIdentityService));

        assertEquals(TaskResult.OK, worker.handleOrderRequest());
        assertNull(worker.getStatusReason());
    }

    @Test
    public void taskWorker_withoutEquivalence_shouldBeRejectedWithLoincReason() throws Exception {
        ITestIdentityService testIdentityService = mock(ITestIdentityService.class);
        TaskWorker worker = worker(
                newInterpreter(labWithTestCodedAs("25836-8", testIdentityService), testIdentityService));

        assertEquals(TaskResult.MESSAGE_ERROR, worker.handleOrderRequest());
        assertEquals("examen non reconnu : LOINC 20447-9", worker.getStatusReason());
        assertFalse(worker.getStatusReason().contains("DEMO"));
    }

    @Test
    public void statusReason_rejectedTask_shouldCarryReasonText() {
        CodeableConcept reason = FhirApiWorkFlowServiceImpl.statusReasonFor(TaskStatus.REJECTED,
                new LinkedHashSet<>(Arrays.asList("examen non reconnu : LOINC 20447-9")));
        assertEquals("examen non reconnu : LOINC 20447-9", reason.getText());
        assertNull(FhirApiWorkFlowServiceImpl.statusReasonFor(TaskStatus.ACCEPTED, new LinkedHashSet<>()));
        assertEquals("demande non conforme",
                FhirApiWorkFlowServiceImpl.statusReasonFor(TaskStatus.REJECTED, new LinkedHashSet<>()).getText());
    }

    @Test
    public void loincEquivalences_shouldParsePairs() {
        assertEquals("25836-8",
                TaskInterpreterImpl.parseLoincEquivalences(" 20447-9 = 25836-8 ; 1-1=2-2").get("20447-9"));
        assertEquals("2-2", TaskInterpreterImpl.parseLoincEquivalences("20447-9=25836-8,1-1=2-2").get("1-1"));
        assertTrue(TaskInterpreterImpl.parseLoincEquivalences("").isEmpty());
        assertTrue(TaskInterpreterImpl.parseLoincEquivalences("mal-forme").isEmpty());
    }

    // ---------------------------------------------------------- dates (L3)

    private Observation observationFor(Timestamp sampleItemCollected, Timestamp sampleCollected,
            java.sql.Date released) throws Exception {
        when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("6");
        when(statusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn("1");

        Sample sample = mock(Sample.class);
        when(sample.getCollectionDate()).thenReturn(sampleCollected);
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("11");
        sampleItem.setSample(sample);
        sampleItem.setFhirUuid(UUID.randomUUID());
        sampleItem.setCollectionDate(sampleItemCollected);

        Analysis analysis = mock(Analysis.class);
        when(analysis.getStatusId()).thenReturn("6");
        when(analysis.getSampleItem()).thenReturn(sampleItem);
        when(analysis.getReleasedDate()).thenReturn(released);
        when(analysis.getFhirUuidAsString()).thenReturn(UUID.randomUUID().toString());

        Result result = new Result();
        result.setId("71");
        result.setFhirUuid(UUID.randomUUID());
        result.setAnalysis(analysis);

        Method transform = FhirTransformServiceImpl.class.getDeclaredMethod("transformResultToObservation",
                Result.class);
        transform.setAccessible(true);
        return (Observation) transform.invoke(fhirTransformService, result);
    }

    private static Timestamp atNoon(LocalDate day) {
        return Timestamp.valueOf(day.atTime(12, 0));
    }

    @Test
    public void observation_collectedYesterdayValidatedToday_shouldHaveEffectiveYesterdayIssuedToday()
            throws Exception {
        LocalDate today = LocalDate.now();
        Timestamp yesterday = atNoon(today.minusDays(1));
        java.sql.Date validated = java.sql.Date.valueOf(today);

        Observation observation = observationFor(yesterday, null, validated);

        assertEquals(yesterday.getTime(), ((DateTimeType) observation.getEffective()).getValue().getTime());
        assertEquals(validated.getTime(), observation.getIssued().getTime());

        // nouvelle transformation : mêmes dates
        Observation again = observationFor(yesterday, null, validated);
        assertEquals(yesterday.getTime(), ((DateTimeType) again.getEffective()).getValue().getTime());
        assertEquals(validated.getTime(), again.getIssued().getTime());
    }

    @Test
    public void observation_withoutSampleItemDate_shouldUseOrderCollectionDate() throws Exception {
        Timestamp orderCollected = atNoon(LocalDate.now().minusDays(2));

        Observation observation = observationFor(null, orderCollected, java.sql.Date.valueOf(LocalDate.now()));

        assertEquals(orderCollected.getTime(), ((DateTimeType) observation.getEffective()).getValue().getTime());
    }

    @Test
    public void observation_withoutAnyCollectionDate_shouldHaveNoEffective() throws Exception {
        Observation observation = observationFor(null, null, java.sql.Date.valueOf(LocalDate.now()));

        assertFalse(observation.hasEffective());
        assertNotNull(observation.getIssued());
    }

    // --------------------------------------------------------------- outils

    /** Capture les messages journalisés par LogEvent (log4j2). */
    static final class CapturingAppender extends AbstractAppender {
        private final List<String> messages = Collections.synchronizedList(new ArrayList<>());
        private LoggerConfig loggerConfig;

        private CapturingAppender() {
            super("cmu-test-capture", null, null, true, Property.EMPTY_ARRAY);
        }

        static CapturingAppender attach() {
            CapturingAppender appender = new CapturingAppender();
            appender.start();
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration configuration = context.getConfiguration();
            String name = org.openelisglobal.common.log.LogEvent.class.getName();
            LoggerConfig config = new LoggerConfig(name, Level.ALL, true);
            config.addAppender(appender, Level.ALL, null);
            configuration.addLogger(name, config);
            context.updateLoggers();
            appender.loggerConfig = config;
            return appender;
        }

        void detach() {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.getConfiguration().removeLogger(loggerConfig.getName());
            context.updateLoggers();
            stop();
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }

        List<String> messagesContaining(String text) {
            synchronized (messages) {
                return messages.stream().filter(m -> m.contains(text)).collect(Collectors.toList());
            }
        }

        String allMessages() {
            synchronized (messages) {
                return String.join("\n", messages);
            }
        }
    }
}
