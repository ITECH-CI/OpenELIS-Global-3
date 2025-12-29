
package org.openelisglobal.testunitconversion.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testunitconversion.dao.TestDerivedFormulaDAO;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedDependency;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedFormula;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Result update hook that automatically converts result values to SI units.
 * Handles both simple conversions and derived formula calculations.
 */
@Component
public class SiUnitConversionUpdate implements IResultUpdate {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SiConversionService siConversionService;

    @Autowired
    private FormulaEvaluationService formulaEvaluationService;

    @Autowired
    private TestDerivedFormulaDAO testDerivedFormulaDAO;

    @Override
    public void transactionalUpdate(IResultSaveService resultService) throws LIMSRuntimeException {
        try {
            convertSimpleResults(resultService);

            calculateDerivedFormulas(resultService);

        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("SI conversion failed", e);
        }
    }

    @Override
    public void postTransactionalCommitUpdate(IResultSaveService resultService) {
        // Logging or notifications if needed
    }

    /**
     * Convert simple results using direct unit conversion rules.
     */
    private void convertSimpleResults(IResultSaveService resultSaveService) {
        // Process new results
        for (ResultSet resultSet : resultSaveService.getNewResults()) {
            convertAndSaveResult(resultSet.result, resultSaveService.getCurrentUserId());
        }

        // Process modified results
        for (ResultSet resultSet : resultSaveService.getModifiedResults()) {
            convertAndSaveResult(resultSet.result, resultSaveService.getCurrentUserId());
        }
    }

    /**
     * Convert a result. No explicit save needed - Hibernate dirty checking will
     * persist changes.
     */
    private void convertAndSaveResult(Result result, String currentUserId) {

        boolean converted = convertSimpleResult(result);

        if (converted) {
            // Reload the result from the current session and update its SI fields
            // This ensures we're modifying the managed entity that Hibernate is tracking
            try {

                // Find the managed instance of this result in the current session
                Result managedResult = entityManager.find(Result.class, result.getId());

                if (managedResult != null) {
                    // Copy the SI values to the managed entity
                    managedResult.setValueSi(result.getValueSi());
                    managedResult.setUomSi(result.getUomSi());
                    managedResult.setSiRule(result.getSiRule());
                    managedResult.setMinNormalSi(result.getMinNormalSi());
                    managedResult.setMaxNormalSi(result.getMaxNormalSi());
                    managedResult.setSiLastupdated(result.getSiLastupdated());

                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "convertAndSaveResult",
                            "Could not find managed result with ID: " + result.getId());
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "convertAndSaveResult",
                        "Error updating managed result: " + e.getMessage());
                LogEvent.logError(e);
            }
        } else {
            LogEvent.logInfo(this.getClass().getSimpleName(), "convertAndSaveResult", "Result was not converted");
        }
    }

    /**
     * Convert a single result to SI units.
     * 
     * @return true if conversion was successful, false otherwise
     */
    private boolean convertSimpleResult(Result result) {
        if (result == null || result.getValue() == null || !"N".equals(result.getResultType())) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "convertSimpleResult",
                    "Skipping result - null, no value, or not numeric. ResultType: "
                            + (result != null ? result.getResultType() : "null"));
            return false;
        }

        // Perform SI conversion
        boolean converted = siConversionService.convertResultToSi(result);

        if (converted) {

            // Also convert reference ranges if present
            Analysis analysis = result.getAnalysis();
            if (analysis != null && analysis.getTest() != null) {
                if (result.getMinNormal() != null || result.getMaxNormal() != null) {
                    SiConversionService.ReferenceRangeConversion rangeConversion = siConversionService
                            .convertReferenceRange(analysis.getTest(), result.getMinNormal(), result.getMaxNormal());

                    if (rangeConversion != null) {
                        result.setMinNormalSi(rangeConversion.minSi);
                        result.setMaxNormalSi(rangeConversion.maxSi);
                    }
                }
            }
            return true;
        } else {
            LogEvent.logInfo(this.getClass().getSimpleName(), "convertSimpleResult",
                    "Conversion failed or no conversion rule found for result");
            return false;
        }
    }

    /**
     * Calculate derived formulas for tests that depend on updated results.
     */
    private void calculateDerivedFormulas(IResultSaveService resultService) {

        // Collect all tests that have been updated
        Set<Test> updatedTests = new HashSet<>();

        for (ResultSet resultSet : resultService.getNewResults()) {
            if (resultSet.result.getAnalysis() != null && resultSet.result.getAnalysis().getTest() != null) {
                updatedTests.add(resultSet.result.getAnalysis().getTest());
            }
        }

        for (ResultSet resultSet : resultService.getModifiedResults()) {
            if (resultSet.result.getAnalysis() != null && resultSet.result.getAnalysis().getTest() != null) {
                updatedTests.add(resultSet.result.getAnalysis().getTest());
            }
        }

        // Find derived formulas that depend on any of the updated tests
        Set<TestDerivedFormula> formulasToCalculate = new HashSet<>();
        for (Test test : updatedTests) {
            List<TestDerivedFormula> formulas = testDerivedFormulaDAO.findBySourceTestId(test.getId());
            if (formulas != null && !formulas.isEmpty()) {

                formulasToCalculate.addAll(formulas);
            }
        }

        // Calculate each derived formula
        for (TestDerivedFormula formula : formulasToCalculate) {
            calculateDerivedFormula(formula, resultService);
        }

    }

    /**
     * Calculate a single derived formula.
     */
    private void calculateDerivedFormula(TestDerivedFormula formula, IResultSaveService resultService) {
        try {
            // Build a map of source test -> result
            Map<Test, Result> sourceResults = new HashMap<>();

            for (TestDerivedDependency dependency : formula.getDependencies()) {
                Test sourceTest = dependency.getSourceTest();

                // Find the result for this source test
                Result sourceResult = findResultForTest(sourceTest, resultService);
                if (sourceResult == null) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "calculateDerivedFormula",
                            "Missing source result for test: " + sourceTest.getId());
                    return; // Can't calculate without all dependencies
                }

                sourceResults.put(sourceTest, sourceResult);
            }

            // Evaluate the formula
            BigDecimal siValue = formulaEvaluationService.evaluateFormula(formula, sourceResults);

            if (siValue != null) {
                // Find the result for the derived test
                Result derivedResult = findResultForTest(formula.getTest(), resultService);

                if (derivedResult != null) {
                    // Apply rounding if specified
                    if (formula.getDecimals() != null && formula.getDecimals() >= 0) {
                        siValue = siValue.setScale(formula.getDecimals(), java.math.RoundingMode.HALF_UP);
                    }

                    // Get the managed entity and update it
                    Result managedResult = entityManager.find(Result.class, derivedResult.getId());
                    if (managedResult != null) {
                        managedResult.setValueSi(siValue.toPlainString());
                        managedResult.setUomSi(formula.getToUomSi());
                        managedResult.setSiLastupdated(new Timestamp(System.currentTimeMillis()));

                    } else {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "calculateDerivedFormula",
                                "Could not find managed result for derived test: " + formula.getTest().getId());
                    }
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "calculateDerivedFormula",
                            "No result found for derived test: " + formula.getTest().getId());
                }
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "calculateDerivedFormula",
                        "Formula evaluation returned null for test: " + formula.getTest().getId());
            }

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "calculateDerivedFormula",
                    "Error calculating derived formula for test " + formula.getTest().getId() + ": " + e.getMessage());
            LogEvent.logError(e);
        }
    }

    /**
     * Find a result for a specific test in the result service.
     */
    private Result findResultForTest(Test test, IResultSaveService resultService) {
        // Search in new results
        for (ResultSet resultSet : resultService.getNewResults()) {
            if (resultSet.result.getAnalysis() != null && resultSet.result.getAnalysis().getTest() != null
                    && resultSet.result.getAnalysis().getTest().getId().equals(test.getId())) {
                return resultSet.result;
            }
        }

        // Search in modified results
        for (ResultSet resultSet : resultService.getModifiedResults()) {
            if (resultSet.result.getAnalysis() != null && resultSet.result.getAnalysis().getTest() != null
                    && resultSet.result.getAnalysis().getTest().getId().equals(test.getId())) {
                return resultSet.result;
            }
        }

        return null;
    }

    /**
     * Get total count of results being processed.
     */
    private int getTotalResultCount(IResultSaveService resultService) {
        return resultService.getNewResults().size() + resultService.getModifiedResults().size();
    }
}
