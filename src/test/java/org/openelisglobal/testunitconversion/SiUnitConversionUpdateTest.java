package org.openelisglobal.testunitconversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.testunitconversion.service.FormulaEvaluationService;
import org.openelisglobal.testunitconversion.service.SiConversionService;
import org.openelisglobal.testunitconversion.service.SiUnitConversionUpdate;

/**
 * Test class for SiUnitConversionUpdate. Tests the integration hook that
 * automatically converts results to SI units.
 */
@RunWith(MockitoJUnitRunner.class)
public class SiUnitConversionUpdateTest {

    @Mock
    private SiConversionService siConversionService;

    @Mock
    private FormulaEvaluationService formulaEvaluationService;

    @Mock
    private IResultSaveService resultSaveService;

    @InjectMocks
    private SiUnitConversionUpdate siUnitConversionUpdate;

    private org.openelisglobal.test.valueholder.Test testEntity;
    private Analysis analysis;
    private Patient patient;
    private Sample sample;

    @Before
    public void setUp() {
        testEntity = new org.openelisglobal.test.valueholder.Test();
        testEntity.setId("1");
        testEntity.setDescription("Hemoglobin");

        analysis = new Analysis();
        analysis.setTest(testEntity);

        patient = new Patient();
        sample = new Sample();
    }

    @Test
    public void testTransactionalUpdate_ConvertsNewResults() {
        // Given: New result to save
        Result newResult = new Result();
        newResult.setValue("12.5");
        newResult.setResultType("N");
        newResult.setAnalysis(analysis);

        ResultSet resultSet = new ResultSet(newResult, null, null, patient, sample, new HashMap<>(), false);

        List<ResultSet> newResults = Arrays.asList(resultSet);
        when(resultSaveService.getNewResults()).thenReturn(newResults);
        when(resultSaveService.getModifiedResults()).thenReturn(Collections.emptyList());
        when(siConversionService.convertResultToSi(newResult)).thenReturn(true);

        // When: Process update
        siUnitConversionUpdate.transactionalUpdate(resultSaveService);

        // Then: Conversion should be called
        verify(siConversionService, times(1)).convertResultToSi(newResult);
    }

    @Test
    public void testTransactionalUpdate_ConvertsModifiedResults() {
        // Given: Modified result
        Result modifiedResult = new Result();
        modifiedResult.setValue("15.0");
        modifiedResult.setResultType("N");
        modifiedResult.setAnalysis(analysis);

        ResultSet resultSet = new ResultSet(modifiedResult, null, null, patient, sample, new HashMap<>(), false);

        List<ResultSet> modifiedResults = Arrays.asList(resultSet);
        when(resultSaveService.getNewResults()).thenReturn(Collections.emptyList());
        when(resultSaveService.getModifiedResults()).thenReturn(modifiedResults);
        when(siConversionService.convertResultToSi(modifiedResult)).thenReturn(true);

        // When: Process update
        siUnitConversionUpdate.transactionalUpdate(resultSaveService);

        // Then: Conversion should be called
        verify(siConversionService, times(1)).convertResultToSi(modifiedResult);
    }

    @Test
    public void testTransactionalUpdate_ConvertsMultipleResults() {
        // Given: Multiple new and modified results
        Result newResult1 = new Result();
        newResult1.setValue("12.5");
        newResult1.setResultType("N");
        newResult1.setAnalysis(analysis);

        Result newResult2 = new Result();
        newResult2.setValue("13.5");
        newResult2.setResultType("N");
        newResult2.setAnalysis(analysis);

        Result modifiedResult = new Result();
        modifiedResult.setValue("14.5");
        modifiedResult.setResultType("N");
        modifiedResult.setAnalysis(analysis);

        ResultSet rs1 = new ResultSet(newResult1, null, null, patient, sample, new HashMap<>(), false);
        ResultSet rs2 = new ResultSet(newResult2, null, null, patient, sample, new HashMap<>(), false);
        ResultSet rs3 = new ResultSet(modifiedResult, null, null, patient, sample, new HashMap<>(), false);

        when(resultSaveService.getNewResults()).thenReturn(Arrays.asList(rs1, rs2));
        when(resultSaveService.getModifiedResults()).thenReturn(Arrays.asList(rs3));
        when(siConversionService.convertResultToSi(any(Result.class))).thenReturn(true);

        // When: Process update
        siUnitConversionUpdate.transactionalUpdate(resultSaveService);

        // Then: All results should be converted
        verify(siConversionService, times(3)).convertResultToSi(any(Result.class));
        verify(siConversionService).convertResultToSi(newResult1);
        verify(siConversionService).convertResultToSi(newResult2);
        verify(siConversionService).convertResultToSi(modifiedResult);
    }

    @Test
    public void testTransactionalUpdate_SkipsNonNumericResults() {
        // Given: Non-numeric result
        Result dictionaryResult = new Result();
        dictionaryResult.setValue("Positive");
        dictionaryResult.setResultType("D"); // Dictionary
        dictionaryResult.setAnalysis(analysis);

        ResultSet resultSet = new ResultSet(dictionaryResult, null, null, patient, sample, new HashMap<>(), false);

        when(resultSaveService.getNewResults()).thenReturn(Arrays.asList(resultSet));
        when(resultSaveService.getModifiedResults()).thenReturn(Collections.emptyList());

        // When: Process update
        siUnitConversionUpdate.transactionalUpdate(resultSaveService);

        // Then: Conversion should be attempted (service will handle non-numeric)
        verify(siConversionService, times(1)).convertResultToSi(dictionaryResult);
    }

    @Test
    public void testTransactionalUpdate_ConvertsReferenceRanges() {
        // Given: Result with reference ranges
        Result result = new Result();
        result.setValue("12.5");
        result.setResultType("N");
        result.setAnalysis(analysis);
        result.setMinNormal(12.0);
        result.setMaxNormal(16.0);

        ResultSet resultSet = new ResultSet(result, null, null, patient, sample, new HashMap<>(), false);

        when(resultSaveService.getNewResults()).thenReturn(Arrays.asList(resultSet));
        when(resultSaveService.getModifiedResults()).thenReturn(Collections.emptyList());
        when(siConversionService.convertResultToSi(result)).thenReturn(true);

        SiConversionService.ReferenceRangeConversion rangeConversion = new SiConversionService.ReferenceRangeConversion(
                120.0, 160.0);
        when(siConversionService.convertReferenceRange(any(org.openelisglobal.test.valueholder.Test.class),
                any(Double.class), any(Double.class))).thenReturn(rangeConversion);

        // When: Process update
        siUnitConversionUpdate.transactionalUpdate(resultSaveService);

        // Then: Both result and reference range should be converted
        verify(siConversionService).convertResultToSi(result);
        verify(siConversionService).convertReferenceRange(testEntity, 12.0, 16.0);
        assertEquals("Min normal SI should be set", 120.0, result.getMinNormalSi(), 0.01);
        assertEquals("Max normal SI should be set", 160.0, result.getMaxNormalSi(), 0.01);
    }

    @Test
    public void testTransactionalUpdate_HandlesConversionFailure() {
        // Given: Result that fails conversion
        Result result = new Result();
        result.setValue("invalid");
        result.setResultType("N");
        result.setAnalysis(analysis);

        ResultSet resultSet = new ResultSet(result, null, null, patient, sample, new HashMap<>(), false);

        when(resultSaveService.getNewResults()).thenReturn(Arrays.asList(resultSet));
        when(resultSaveService.getModifiedResults()).thenReturn(Collections.emptyList());
        when(siConversionService.convertResultToSi(result)).thenReturn(false);

        // When: Process update (should not throw exception)
        siUnitConversionUpdate.transactionalUpdate(resultSaveService);

        // Then: Should handle gracefully
        verify(siConversionService).convertResultToSi(result);
        assertNull("SI value should remain null", result.getValueSi());
    }

    @Test
    public void testTransactionalUpdate_WithNoResults() {
        // Given: No results to process
        when(resultSaveService.getNewResults()).thenReturn(Collections.emptyList());
        when(resultSaveService.getModifiedResults()).thenReturn(Collections.emptyList());

        // When: Process update
        siUnitConversionUpdate.transactionalUpdate(resultSaveService);

        // Then: No conversions should be attempted
        verify(siConversionService, never()).convertResultToSi(any(Result.class));
    }

    @Test
    public void testPostTransactionalCommitUpdate_CompletesSuccessfully() {
        // Given: Any result save service
        when(resultSaveService.getNewResults()).thenReturn(Collections.emptyList());
        when(resultSaveService.getModifiedResults()).thenReturn(Collections.emptyList());

        // When: Post-commit hook is called (currently does nothing)
        siUnitConversionUpdate.postTransactionalCommitUpdate(resultSaveService);

        // Then: Should complete without error
        // This is a placeholder test for future functionality
    }

    @Test
    public void testTransactionalUpdate_PreservesOriginalValue() {
        // Given: Result with original value
        Result result = new Result();
        result.setValue("12.5");
        result.setResultType("N");
        result.setAnalysis(analysis);

        ResultSet resultSet = new ResultSet(result, null, null, patient, sample, new HashMap<>(), false);

        when(resultSaveService.getNewResults()).thenReturn(Arrays.asList(resultSet));
        when(resultSaveService.getModifiedResults()).thenReturn(Collections.emptyList());
        when(siConversionService.convertResultToSi(result)).thenReturn(true);

        // When: Process update
        siUnitConversionUpdate.transactionalUpdate(resultSaveService);

        // Then: Original value should be unchanged
        assertEquals("Original value should be preserved", "12.5", result.getValue());
    }
}
