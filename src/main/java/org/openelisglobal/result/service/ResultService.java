package org.openelisglobal.result.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.valueholder.TestResult;

public interface ResultService extends BaseObjectService<Result, String> {
    void getData(Result result);

    List<Result> getResultsForTestSectionInDateRange(String testSectionId, Date lowDate, Date highDate);

    void getResultByAnalysisAndAnalyte(Result result, Analysis analysis, TestAnalyte ta);

    List<Result> getResultsByAnalysis(Analysis analysis);

    /**
     * Read the per-result UoM override id (result.uom_id) without initializing the
     * lazy uom association. Returns null when no override is set.
     */
    String getUomIdForResult(String resultId);

    List<Result> getResultsForAnalysisIdList(List<Integer> analysisIdList);

    List<Result> getResultsForPanelInDateRange(String panelId, Date lowDate, Date highDate);

    List<Result> getResultsForSample(Sample sample);

    Result getResultForAnalyteInAnalysisSet(String analyteId, List<Integer> analysisIDList);

    List<Result> getResultsForTestInDateRange(String testId, Date startDate, Date endDate);

    void getResultByTestResult(Result result, TestResult testResult);

    Result getResultForAnalyteAndSampleItem(String analyteId, String sampleItemId);

    List<Result> getResultsForTestAndSample(String sampleId, String testId);

    List<Result> getReportableResultsByAnalysis(Analysis analysis);

    List<Result> getChildResults(String resultId);

    List<Result> getAllResults();

    Result getResultById(Result result);

    Result getResultById(String resultId);

    List<Result> getPageOfResults(int startingRecNo);

    String getSignature(Result result);

    String getLastUpdatedTime(Result result);

    boolean isAbnormalDictionaryResult(Result result);

    String getDisplayReferenceRange(Result result, boolean includeSelectList);

    String getResultValue(Result result, String separator, boolean printable, boolean includeUOM);

    String getSimpleResultValue(Result result);

    String getTestType(Result result);

    String getTestTime(Result result);

    String getLOINCCode(Result result);

    String getTestDescription(Result result);

    String getReportingTestName(Result result);

    String getResultValue(Result result, boolean printable);

    String getResultValueForDisplay(Result result, String string, boolean b, boolean c);

    String getUOM(Result result);

    // SI Unit Conversion methods

    /**
     * Get the SI converted value for a result.
     * 
     * @param result The result
     * @return The SI value as a string, or null if not available
     */
    String getSiValue(Result result);

    /**
     * Get the SI unit of measure for a result.
     * 
     * @param result The result
     * @return The SI unit name, or empty string if not available
     */
    String getSiUom(Result result);

    /**
     * Get the formatted result value with SI value in parentheses if available.
     * Format: "12.5 g/dL (125.0 g/L)" or just "12.5 g/dL" if no SI conversion
     * 
     * @param result     The result
     * @param separator  Separator between value and unit
     * @param printable  Whether to format for printing
     * @param includeUOM Whether to include the unit of measure
     * @return Formatted result value with optional SI value
     */
    String getFormattedValueWithSi(Result result, String separator, boolean printable, boolean includeUOM);

    /**
     * Check if a result has an SI conversion available.
     * 
     * @param result The result
     * @return true if SI value exists, false otherwise
     */
    boolean hasSiConversion(Result result);

    /**
     * Get the formatted SI reference range.
     * 
     * @param result The result with SI reference range values
     * @return Formatted SI reference range, or empty string if not available
     */
    String getSiReferenceRange(Result result);
}
