package org.openelisglobal.testunitconversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testunitconversion.dao.TestAliasDAO;
import org.openelisglobal.testunitconversion.dao.TestDerivedFormulaDAO;
import org.openelisglobal.testunitconversion.service.FormulaEvaluationService;
import org.openelisglobal.testunitconversion.valueholder.TestAlias;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedFormula;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class for FormulaEvaluationService. Tests the evaluation of mathematical
 * formulas for derived test results.
 */
public class FormulaEvaluationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private FormulaEvaluationService formulaEvaluationService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestAliasDAO testAliasDAO;

    @Autowired
    private TestDerivedFormulaDAO testDerivedFormulaDAO;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    private org.openelisglobal.test.valueholder.Test hbTest;
    private org.openelisglobal.test.valueholder.Test hctTest;
    private org.openelisglobal.test.valueholder.Test mchcTest;

    private UnitOfMeasure gPerL;
    private UnitOfMeasure percent;
    private UnitOfMeasure gPerDL;

    @Before
    public void setUp() {
        // Create units of measure
        gPerL = new UnitOfMeasure();
        gPerL.setUnitOfMeasureName("g/L");
        gPerL.setDescription("Grams per liter");
        unitOfMeasureService.insert(gPerL);

        percent = new UnitOfMeasure();
        percent.setUnitOfMeasureName("%");
        percent.setDescription("Percentage");
        unitOfMeasureService.insert(percent);

        gPerDL = new UnitOfMeasure();
        gPerDL.setUnitOfMeasureName("g/dL");
        gPerDL.setDescription("Grams per deciliter");
        unitOfMeasureService.insert(gPerDL);

        // Create tests
        hbTest = new org.openelisglobal.test.valueholder.Test();
        hbTest.setDescription("Hemoglobin Test");
        testService.insert(hbTest);

        hctTest = new org.openelisglobal.test.valueholder.Test();
        hctTest.setDescription("Hematocrit Test");
        testService.insert(hctTest);

        mchcTest = new org.openelisglobal.test.valueholder.Test();
        mchcTest.setDescription("Mean Corpuscular Hemoglobin Concentration");
        testService.insert(mchcTest);

        // Create aliases
        TestAlias hbAlias = new TestAlias();
        hbAlias.setTest(hbTest);
        hbAlias.setAlias("Hb");
        testAliasDAO.insert(hbAlias);

        TestAlias hctAlias = new TestAlias();
        hctAlias.setTest(hctTest);
        hctAlias.setAlias("HCT");
        testAliasDAO.insert(hctAlias);
    }

    @Test
    public void testSimpleFormula_MultiplicationAndDivision() {
        // Given: MCHC = (Hb * 100) / HCT
        TestDerivedFormula formula = new TestDerivedFormula();
        formula.setTest(mchcTest);
        formula.setExpression("(Hb * 100) / HCT");
        formula.setFromUom(gPerL);
        formula.setToUomSi(gPerDL);
        formula.setDecimals(1);
        formula.setActive(true);
        testDerivedFormulaDAO.insert(formula);

        // Source results: Hb = 150 g/L, HCT = 45%
        Map<org.openelisglobal.test.valueholder.Test, Result> sourceResults = new HashMap<>();

        Result hbResult = new Result();
        hbResult.setValueSi("150");
        sourceResults.put(hbTest, hbResult);

        Result hctResult = new Result();
        hctResult.setValueSi("45");
        sourceResults.put(hctTest, hctResult);

        // When: Evaluate formula
        BigDecimal result = formulaEvaluationService.evaluateFormula(formula, sourceResults);

        // Then: MCHC = (150 * 100) / 45 = 333.3 g/dL
        assertNotNull("Result should not be null", result);
        assertEquals("MCHC should be 333.3", new BigDecimal("333.3"), result);
    }

    @Test
    public void testFormulaWithAdditionAndSubtraction() {
        // Given: Formula with addition and subtraction
        TestDerivedFormula formula = new TestDerivedFormula();
        formula.setTest(mchcTest);
        formula.setExpression("Hb + HCT - 50");
        formula.setFromUom(gPerL);
        formula.setToUomSi(gPerDL);
        formula.setDecimals(2);
        formula.setActive(true);

        Map<org.openelisglobal.test.valueholder.Test, Result> sourceResults = new HashMap<>();

        Result hbResult = new Result();
        hbResult.setValueSi("150");
        sourceResults.put(hbTest, hbResult);

        Result hctResult = new Result();
        hctResult.setValueSi("45");
        sourceResults.put(hctTest, hctResult);

        // When: Evaluate
        BigDecimal result = formulaEvaluationService.evaluateFormula(formula, sourceResults);

        // Then: 150 + 45 - 50 = 145
        assertNotNull("Result should not be null", result);
        assertEquals("Result should be 145.00", new BigDecimal("145.00"), result);
    }

    @Test
    public void testFormulaWithParentheses() {
        // Given: Formula with nested parentheses
        TestDerivedFormula formula = new TestDerivedFormula();
        formula.setTest(mchcTest);
        formula.setExpression("((Hb + 10) * 2) / (HCT - 5)");
        formula.setFromUom(gPerL);
        formula.setToUomSi(gPerDL);
        formula.setDecimals(2);
        formula.setActive(true);

        Map<org.openelisglobal.test.valueholder.Test, Result> sourceResults = new HashMap<>();

        Result hbResult = new Result();
        hbResult.setValueSi("150");
        sourceResults.put(hbTest, hbResult);

        Result hctResult = new Result();
        hctResult.setValueSi("45");
        sourceResults.put(hctTest, hctResult);

        // When: Evaluate: ((150 + 10) * 2) / (45 - 5) = (160 * 2) / 40 = 320 / 40 = 8
        BigDecimal result = formulaEvaluationService.evaluateFormula(formula, sourceResults);

        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Result should be 8.00", new BigDecimal("8.00"), result);
    }

    @Test
    public void testFormulaWithDecimalNumbers() {
        // Given: Formula with decimal constants
        TestDerivedFormula formula = new TestDerivedFormula();
        formula.setTest(mchcTest);
        formula.setExpression("Hb * 0.621");
        formula.setFromUom(gPerL);
        formula.setToUomSi(gPerDL);
        formula.setDecimals(2);
        formula.setActive(true);

        Map<org.openelisglobal.test.valueholder.Test, Result> sourceResults = new HashMap<>();

        Result hbResult = new Result();
        hbResult.setValueSi("150");
        sourceResults.put(hbTest, hbResult);

        // When: Evaluate: 150 * 0.621 = 93.15
        BigDecimal result = formulaEvaluationService.evaluateFormula(formula, sourceResults);

        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Result should be 93.15", new BigDecimal("93.15"), result);
    }

    @Test
    public void testFormulaWithMissingSourceValue_ReturnsNull() {
        // Given: Formula but missing source result
        TestDerivedFormula formula = new TestDerivedFormula();
        formula.setTest(mchcTest);
        formula.setExpression("Hb * 100 / HCT");
        formula.setFromUom(gPerL);
        formula.setToUomSi(gPerDL);
        formula.setDecimals(1);
        formula.setActive(true);

        Map<org.openelisglobal.test.valueholder.Test, Result> sourceResults = new HashMap<>();

        // Only provide Hb, missing HCT
        Result hbResult = new Result();
        hbResult.setValueSi("150");
        sourceResults.put(hbTest, hbResult);

        // When: Try to evaluate
        BigDecimal result = formulaEvaluationService.evaluateFormula(formula, sourceResults);

        // Then: Should return null or handle gracefully
        // Note: Implementation should handle this gracefully
        // For now, we expect null or an exception to be caught
        assertTrue("Should handle missing source gracefully", result == null);
    }

    @Test
    public void testFormulaWithInvalidExpression_ReturnsNull() {
        // Given: Invalid formula syntax
        TestDerivedFormula formula = new TestDerivedFormula();
        formula.setTest(mchcTest);
        formula.setExpression("Hb * / HCT"); // Invalid syntax
        formula.setFromUom(gPerL);
        formula.setToUomSi(gPerDL);
        formula.setDecimals(1);
        formula.setActive(true);

        Map<org.openelisglobal.test.valueholder.Test, Result> sourceResults = new HashMap<>();

        Result hbResult = new Result();
        hbResult.setValueSi("150");
        sourceResults.put(hbTest, hbResult);

        Result hctResult = new Result();
        hctResult.setValueSi("45");
        sourceResults.put(hctTest, hctResult);

        // When: Try to evaluate
        BigDecimal result = formulaEvaluationService.evaluateFormula(formula, sourceResults);

        // Then: Should return null due to parse error
        assertNull("Invalid expression should return null", result);
    }

    @Test
    public void testFormulaWithDivisionByZero_ReturnsNull() {
        // Given: Formula that results in division by zero
        TestDerivedFormula formula = new TestDerivedFormula();
        formula.setTest(mchcTest);
        formula.setExpression("Hb / (HCT - 45)"); // Will divide by zero
        formula.setFromUom(gPerL);
        formula.setToUomSi(gPerDL);
        formula.setDecimals(1);
        formula.setActive(true);

        Map<org.openelisglobal.test.valueholder.Test, Result> sourceResults = new HashMap<>();

        Result hbResult = new Result();
        hbResult.setValueSi("150");
        sourceResults.put(hbTest, hbResult);

        Result hctResult = new Result();
        hctResult.setValueSi("45"); // This will make HCT - 45 = 0
        sourceResults.put(hctTest, hctResult);

        // When: Try to evaluate
        BigDecimal result = formulaEvaluationService.evaluateFormula(formula, sourceResults);

        // Then: Should handle division by zero gracefully
        assertNull("Division by zero should return null", result);
    }

    @Test
    public void testComplexFormula_WithMultipleOperators() {
        // Given: A more complex formula
        TestDerivedFormula formula = new TestDerivedFormula();
        formula.setTest(mchcTest);
        formula.setExpression("(Hb * 10 + HCT * 2) / (Hb - HCT)");
        formula.setFromUom(gPerL);
        formula.setToUomSi(gPerDL);
        formula.setDecimals(2);
        formula.setActive(true);

        Map<org.openelisglobal.test.valueholder.Test, Result> sourceResults = new HashMap<>();

        Result hbResult = new Result();
        hbResult.setValueSi("150");
        sourceResults.put(hbTest, hbResult);

        Result hctResult = new Result();
        hctResult.setValueSi("45");
        sourceResults.put(hctTest, hctResult);

        // When: Evaluate: (150*10 + 45*2) / (150-45) = (1500 + 90) / 105 = 1590 / 105 =
        // 15.14...
        BigDecimal result = formulaEvaluationService.evaluateFormula(formula, sourceResults);

        // Then
        assertNotNull("Result should not be null", result);
        // 1590 / 105 = 15.142857... rounded to 2 decimals = 15.14
        assertEquals("Result should be 15.14", new BigDecimal("15.14"), result);
    }
}
