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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Résultat corrigé : la nouvelle valeur d'un résultat remis « à valider » n'est
 * pas émise ; un résultat marqué corrigé et validé sort en corrected, son
 * compte rendu aussi, avec les mêmes identifiants.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FhirResultCorrectionTest {

    private static final Map<AnalysisStatus, String> STATUS_IDS = new HashMap<>();
    static {
        STATUS_IDS.put(AnalysisStatus.NotStarted, "4");
        STATUS_IDS.put(AnalysisStatus.Finalized, "6");
        STATUS_IDS.put(AnalysisStatus.BiologistRejected, "7");
        STATUS_IDS.put(AnalysisStatus.Canceled, "14");
        STATUS_IDS.put(AnalysisStatus.TechnicalAcceptance, "15");
        STATUS_IDS.put(AnalysisStatus.TechnicalRejected, "16");
        STATUS_IDS.put(AnalysisStatus.SampleRejected, "26");
    }

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private IStatusService statusService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private ResultService resultService;
    @Mock
    private org.openelisglobal.samplehuman.service.SampleHumanService sampleHumanService;
    @Mock
    private org.openelisglobal.siteinformation.service.SiteInformationService siteInformationService;

    @InjectMocks
    private FhirTransformServiceImpl fhirTransformService;

    private TableIdService previousTableIdService;
    private SampleItem sampleItem;

    @Before
    public void setUp() throws Exception {
        when(fhirConfig.getOeFhirSystem()).thenReturn("http://openelis-global.org");
        when(statusService.getStatusID(any(AnalysisStatus.class)))
                .thenAnswer(invocation -> STATUS_IDS.get(invocation.getArgument(0)));
        when(statusService.matches(anyString(), any(AnalysisStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).equals(STATUS_IDS.get(invocation.getArgument(1))));
        previousTableIdService = swapTableIdService(mock(TableIdService.class));

        Sample sample = mock(Sample.class);
        when(sample.getCollectionDate()).thenReturn(Timestamp.valueOf("2026-11-10 09:00:00"));
        sampleItem = new SampleItem();
        sampleItem.setId("11");
        sampleItem.setSample(sample);
        sampleItem.setFhirUuid(UUID.randomUUID());
    }

    @After
    public void tearDown() throws Exception {
        swapTableIdService(previousTableIdService);
    }

    private static TableIdService swapTableIdService(TableIdService replacement) throws Exception {
        Field instance = TableIdService.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        TableIdService previous = (TableIdService) instance.get(null);
        instance.set(null, replacement);
        return previous;
    }

    private Analysis analysis(AnalysisStatus status, boolean hasValidationDate) {
        Analysis analysis = mock(Analysis.class);
        when(analysis.getId()).thenReturn("259");
        when(analysis.getStatusId()).thenReturn(STATUS_IDS.get(status));
        when(analysis.getSampleItem()).thenReturn(sampleItem);
        when(analysis.getFhirUuidAsString()).thenReturn("3b0d6d64-0000-4000-8000-000000000259");
        when(analysis.getReleasedDate()).thenReturn(hasValidationDate ? java.sql.Date.valueOf("2026-11-12") : null);
        return analysis;
    }

    private static Result result(Analysis analysis, boolean corrected) {
        Result result = new Result();
        result.setId("212");
        result.setFhirUuid(UUID.fromString("94f6e0b2-0000-4000-8000-000000000212"));
        result.setAnalysis(analysis);
        if (corrected) {
            result.setCorrectionDate(Timestamp.valueOf("2026-11-12 10:00:00"));
        }
        return result;
    }

    private Observation observation(Result result) throws Exception {
        Method transform = FhirTransformServiceImpl.class.getDeclaredMethod("transformResultToObservation",
                Result.class);
        transform.setAccessible(true);
        return (Observation) transform.invoke(fhirTransformService, result);
    }

    // --- garde-fou ---

    @Test
    public void resultUnderRevalidation_shouldBeWithheld() {
        assertTrue(fhirTransformService
                .withheldDuringRevalidation(result(analysis(AnalysisStatus.TechnicalAcceptance, true), true)));
    }

    @Test
    public void resultAwaitingFirstValidation_shouldStillBeEmitted() {
        assertFalse(fhirTransformService
                .withheldDuringRevalidation(result(analysis(AnalysisStatus.TechnicalAcceptance, false), false)));
    }

    @Test
    public void validatedOrCancelledResult_shouldBeEmitted() {
        assertFalse(fhirTransformService
                .withheldDuringRevalidation(result(analysis(AnalysisStatus.Finalized, true), true)));
        assertFalse(fhirTransformService
                .withheldDuringRevalidation(result(analysis(AnalysisStatus.Canceled, true), false)));
    }

    // --- corrected ---

    @Test
    public void correctedAndRevalidatedResult_shouldBeCorrected_sameIdentifiers() throws Exception {
        Observation observation = observation(result(analysis(AnalysisStatus.Finalized, true), true));

        assertEquals(ObservationStatus.CORRECTED, observation.getStatus());
        assertEquals("94f6e0b2-0000-4000-8000-000000000212", observation.getIdElement().getIdPart());
        assertEquals("94f6e0b2-0000-4000-8000-000000000212", observation.getIdentifierFirstRep().getValue());
    }

    @Test
    public void validatedResultWithoutCorrection_shouldBeFinal() throws Exception {
        assertEquals(ObservationStatus.FINAL,
                observation(result(analysis(AnalysisStatus.Finalized, true), false)).getStatus());
    }

    @Test
    public void correctedResult_validateAllResultsFalse_shouldBeCorrectedAtOnce() throws Exception {
        // « valider tous les résultats » = false : la saisie finalise directement
        // l'analyse, sans date de validation ; le résultat marqué sort corrected.
        Analysis autoFinalized = analysis(AnalysisStatus.Finalized, false);
        Result corrected = result(autoFinalized, true);

        assertFalse(fhirTransformService.withheldDuringRevalidation(corrected));
        assertEquals(ObservationStatus.CORRECTED, observation(corrected).getStatus());
    }

    @Test
    public void reportWithACorrectedResult_shouldBeCorrected() {
        Analysis corrected = analysis(AnalysisStatus.Finalized, true);
        Analysis other = analysis(AnalysisStatus.Finalized, true);
        when(analysisService.getAnalysesBySampleItem(sampleItem)).thenReturn(Arrays.asList(corrected, other));
        when(resultService.getResultsByAnalysis(corrected)).thenReturn(Arrays.asList(result(corrected, true)));
        when(resultService.getResultsByAnalysis(other)).thenReturn(Arrays.asList(result(other, false)));

        DiagnosticReport report = fhirTransformService.transformSampleItemToDiagnosticReport(sampleItem, null);

        assertEquals(DiagnosticReportStatus.CORRECTED, report.getStatus());
        assertEquals(sampleItem.getFhirUuidAsString(), report.getIdElement().getIdPart());
    }

    @Test
    public void reportWithoutCorrectedResult_shouldBeFinal() {
        Analysis analysis = analysis(AnalysisStatus.Finalized, true);
        when(analysisService.getAnalysesBySampleItem(sampleItem)).thenReturn(Arrays.asList(analysis));
        when(resultService.getResultsByAnalysis(analysis))
                .thenReturn(Collections.singletonList(result(analysis, false)));

        assertEquals(DiagnosticReportStatus.FINAL,
                fhirTransformService.transformSampleItemToDiagnosticReport(sampleItem, null).getStatus());
    }

    @Test
    public void markedResultStillAwaitingRevalidation_shouldNotMakeReportCorrected() {
        // la correction n'est pas encore revalidée : elle ne compte pas
        Analysis pending = analysis(AnalysisStatus.TechnicalAcceptance, true);
        Analysis validated = analysis(AnalysisStatus.Finalized, true);
        when(analysisService.getAnalysesBySampleItem(sampleItem)).thenReturn(Arrays.asList(pending, validated));
        when(resultService.getResultsByAnalysis(pending)).thenReturn(Arrays.asList(result(pending, true)));
        when(resultService.getResultsByAnalysis(validated)).thenReturn(Arrays.asList(result(validated, false)));

        assertEquals(DiagnosticReportStatus.PARTIAL,
                fhirTransformService.transformSampleItemToDiagnosticReport(sampleItem, null).getStatus());
    }
}
