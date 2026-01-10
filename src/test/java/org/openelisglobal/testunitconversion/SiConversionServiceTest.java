package org.openelisglobal.testunitconversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testunitconversion.dao.TestUnitConversionDAO;
import org.openelisglobal.testunitconversion.service.SiConversionService;
import org.openelisglobal.testunitconversion.valueholder.TestUnitConversion;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class for SiConversionService. Tests the automatic conversion of test
 * results from traditional units to SI units.
 */
public class SiConversionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SiConversionService siConversionService;

    @Autowired
    private TestUnitConversionDAO testUnitConversionDAO;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Autowired
    private TestService testService;

    private org.openelisglobal.test.valueholder.Test testEntity;
    private UnitOfMeasure fromUom;
    private UnitOfMeasure toUomSi;
    private TestUnitConversion conversion;

    @Before
    public void setUp() {
        // Create test units of measure
        fromUom = new UnitOfMeasure();
        fromUom.setUnitOfMeasureName("g/dL");
        fromUom.setDescription("Grams per deciliter");
        unitOfMeasureService.insert(fromUom);

        toUomSi = new UnitOfMeasure();
        toUomSi.setUnitOfMeasureName("g/L");
        toUomSi.setDescription("Grams per liter (SI)");
        unitOfMeasureService.insert(toUomSi);

        // Create a test
        testEntity = new org.openelisglobal.test.valueholder.Test();
        testEntity.setDescription("Hemoglobin Test");
        testService.insert(testEntity);

        // Create a conversion rule: g/dL to g/L (multiply by 10)
        conversion = new TestUnitConversion();
        conversion.setTest(testEntity);
        conversion.setFromUom(fromUom);
        conversion.setToUom(toUomSi);
        conversion.setFactor(new BigDecimal("10"));
        conversion.setOffsetValue(new BigDecimal("0"));
        conversion.setDecimals(1);
        conversion.setActive(true);
        testUnitConversionDAO.insert(conversion);
    }

    @Test
    public void testSimpleConversion_MultipliesCorrectly() {
        // Given: A result with value 12.5 g/dL
        Result result = new Result();
        result.setValue("12.5");
        result.setResultType("N"); // Numeric

        org.openelisglobal.analysis.valueholder.Analysis analysis = new org.openelisglobal.analysis.valueholder.Analysis();
        analysis.setTest(testEntity);
        result.setAnalysis(analysis);

        // When: Convert to SI
        boolean converted = siConversionService.convertResultToSi(result);

        // Then: Value should be 125.0 g/L
        assertTrue("Conversion should succeed", converted);
        assertNotNull("SI value should not be null", result.getValueSi());
        assertEquals("SI value should be 125.0", "125.0", result.getValueSi());
        assertNotNull("SI UOM should be set", result.getUomSi());
        assertEquals("SI UOM should be g/L", "g/L", result.getUomSi().getUnitOfMeasureName());
        assertNotNull("SI rule should be set", result.getSiRule());
        assertNotNull("SI lastupdated should be set", result.getSiLastupdated());
    }

    @Test
    public void testConversionWithOffset() {
        // Given: A conversion with both factor and offset (e.g., Celsius to Kelvin)
        TestUnitConversion tempConversion = new TestUnitConversion();
        tempConversion.setTest(testEntity);
        tempConversion.setFromUom(fromUom);
        tempConversion.setToUom(toUomSi);
        tempConversion.setFactor(new BigDecimal("1"));
        tempConversion.setOffsetValue(new BigDecimal("273.15"));
        tempConversion.setDecimals(2);
        tempConversion.setActive(true);
        testUnitConversionDAO.insert(tempConversion);

        // Clear cache to pick up new conversion
        siConversionService.clearCache();

        Result result = new Result();
        result.setValue("25");
        result.setResultType("N");

        org.openelisglobal.analysis.valueholder.Analysis analysis = new org.openelisglobal.analysis.valueholder.Analysis();
        analysis.setTest(testEntity);
        result.setAnalysis(analysis);

        // When: Convert
        boolean converted = siConversionService.convertResultToSi(result);

        // Then: Value should be 25 + 273.15 = 298.15
        assertTrue("Conversion should succeed", converted);
        assertEquals("SI value should be 298.15", "298.15", result.getValueSi());
    }

    @Test
    public void testConversionWithDecimals_RoundsCorrectly() {
        // Given: A result with many decimals
        Result result = new Result();
        result.setValue("12.3456789");
        result.setResultType("N");

        org.openelisglobal.analysis.valueholder.Analysis analysis = new org.openelisglobal.analysis.valueholder.Analysis();
        analysis.setTest(testEntity);
        result.setAnalysis(analysis);

        // When: Convert (conversion has 1 decimal place)
        boolean converted = siConversionService.convertResultToSi(result);

        // Then: Should round to 1 decimal place (123.5)
        assertTrue("Conversion should succeed", converted);
        assertEquals("SI value should be rounded to 1 decimal", "123.5", result.getValueSi());
    }

    @Test
    public void testNonNumericResult_DoesNotConvert() {
        // Given: A non-numeric result
        Result result = new Result();
        result.setValue("Positive");
        result.setResultType("D"); // Dictionary result

        org.openelisglobal.analysis.valueholder.Analysis analysis = new org.openelisglobal.analysis.valueholder.Analysis();
        analysis.setTest(testEntity);
        result.setAnalysis(analysis);

        // When: Try to convert
        boolean converted = siConversionService.convertResultToSi(result);

        // Then: Should not convert
        assertFalse("Non-numeric result should not be converted", converted);
        assertNull("SI value should be null", result.getValueSi());
    }

    @Test
    public void testNullValue_DoesNotConvert() {
        // Given: A result with null value
        Result result = new Result();
        result.setValue(null);
        result.setResultType("N");

        org.openelisglobal.analysis.valueholder.Analysis analysis = new org.openelisglobal.analysis.valueholder.Analysis();
        analysis.setTest(testEntity);
        result.setAnalysis(analysis);

        // When: Try to convert
        boolean converted = siConversionService.convertResultToSi(result);

        // Then: Should not convert
        assertFalse("Null value should not be converted", converted);
        assertNull("SI value should be null", result.getValueSi());
    }

    @Test
    public void testInactiveConversion_DoesNotConvert() {
        // Given: An inactive conversion rule
        conversion.setActive(false);
        testUnitConversionDAO.update(conversion);
        siConversionService.clearCache();

        Result result = new Result();
        result.setValue("12.5");
        result.setResultType("N");

        org.openelisglobal.analysis.valueholder.Analysis analysis = new org.openelisglobal.analysis.valueholder.Analysis();
        analysis.setTest(testEntity);
        result.setAnalysis(analysis);

        // When: Try to convert
        boolean converted = siConversionService.convertResultToSi(result);

        // Then: Should not convert
        assertFalse("Inactive conversion should not be used", converted);
        assertNull("SI value should be null", result.getValueSi());
    }

    @Test
    public void testConversionWithReferenceRange() {
        // Given: Min and max normal values
        Double minNormal = 12.0;
        Double maxNormal = 16.0;

        // When: Convert reference range
        SiConversionService.ReferenceRangeConversion converted = siConversionService.convertReferenceRange(testEntity,
                minNormal, maxNormal);

        // Then: Should be converted using the same factor (x10)
        assertNotNull("Conversion result should not be null", converted);
        assertNotNull("Min SI should not be null", converted.minSi);
        assertNotNull("Max SI should not be null", converted.maxSi);
        assertEquals("Min should be 120.0", 120.0, converted.minSi, 0.01);
        assertEquals("Max should be 160.0", 160.0, converted.maxSi, 0.01);
    }
}
