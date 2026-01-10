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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.testunitconversion.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testunitconversion.dao.TestUnitConversionDAO;
import org.openelisglobal.testunitconversion.valueholder.TestUnitConversion;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for converting test result values from traditional
 * units to SI units.
 */
@Service
public class SiConversionServiceImpl implements SiConversionService {

    @Autowired
    private TestUnitConversionDAO testUnitConversionDAO;

    // Cache: testId + fromUomId -> TestUnitConversion
    private Map<String, TestUnitConversion> conversionCache = new HashMap<>();

    @Override
    @Transactional(readOnly = true)
    public synchronized boolean convertResultToSi(Result result) {
        try {
            // Only convert numeric results with a value
            if (result == null || result.getValue() == null || !"N".equals(result.getResultType())) {
                return false;
            }

            Analysis analysis = result.getAnalysis();
            if (analysis == null || analysis.getTest() == null) {
                return false;
            }

            Test test = analysis.getTest();
            UnitOfMeasure fromUom = test.getUnitOfMeasure();

            if (fromUom == null) {
                return false;
            }

            // Get conversion rule
            TestUnitConversion conversion = getConversionRule(test.getId(), fromUom.getId());
            if (conversion == null || !conversion.getActive()) {
                return false;
            }

            // Parse the numeric value
            BigDecimal numericValue;
            try {
                String valueStr = result.getValue().trim();

                // Skip if value is empty after trimming
                if (valueStr.isEmpty()) {
                    return false;
                }

                // Check if the value contains only valid numeric characters
                // Valid: 123, -123, 123.45, -123.45, .45, -.45
                if (!valueStr.matches("^-?\\d*\\.?\\d+$")) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "convertResultToSi",
                            "Value is not a valid number, skipping SI conversion: " + result.getValue());
                    return false;
                }

                numericValue = new BigDecimal(valueStr);
            } catch (NumberFormatException | NullPointerException e) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "convertResultToSi",
                        "Could not parse numeric value for SI conversion: " + result.getValue());
                return false;
            }

            // Perform conversion using the rule's convertToSi method
            BigDecimal siValue = conversion.convertToSi(numericValue);

            if (siValue != null) {
                // Update result with SI values
                result.setValueSi(siValue.toPlainString());
                result.setUomSi(conversion.getToUom());
                result.setSiRule(conversion);

                result.setSiLastupdated(new Timestamp(System.currentTimeMillis()));
                return true;
            }

            return false;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "convertResultToSi",
                    "Error during SI conversion: " + e.getMessage());
            LogEvent.logError(e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public synchronized ReferenceRangeConversion convertReferenceRange(Test test, Double minNormal, Double maxNormal) {
        if (test == null || test.getUnitOfMeasure() == null) {
            return new ReferenceRangeConversion(minNormal, maxNormal);
        }

        TestUnitConversion conversion = getConversionRule(test.getId(), test.getUnitOfMeasure().getId());
        if (conversion == null || !conversion.getActive()) {
            return new ReferenceRangeConversion(minNormal, maxNormal);
        }

        Double minSi = null;
        Double maxSi = null;

        try {
            if (minNormal != null) {
                BigDecimal minBd = BigDecimal.valueOf(minNormal);
                BigDecimal minSiBd = conversion.convertToSi(minBd);
                minSi = minSiBd != null ? minSiBd.doubleValue() : minNormal;
            }

            if (maxNormal != null) {
                BigDecimal maxBd = BigDecimal.valueOf(maxNormal);
                BigDecimal maxSiBd = conversion.convertToSi(maxBd);
                maxSi = maxSiBd != null ? maxSiBd.doubleValue() : maxNormal;
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "convertReferenceRange",
                    "Error converting reference range: " + e.getMessage());
            return new ReferenceRangeConversion(minNormal, maxNormal);
        }

        return new ReferenceRangeConversion(minSi, maxSi);
    }

    @Override
    public synchronized void clearCache() {
        conversionCache.clear();
    }

    /**
     * Get conversion rule from cache or database.
     *
     * @param testId    Test ID
     * @param fromUomId Source unit of measure ID
     * @return TestUnitConversion or null if not found
     */
    private synchronized TestUnitConversion getConversionRule(String testId, String fromUomId) {
        String cacheKey = testId + ":" + fromUomId;

        TestUnitConversion conversion = conversionCache.get(cacheKey);
        if (conversion != null) {
            return conversion;
        }

        // Load from database
        conversion = testUnitConversionDAO.findByTestAndFromUom(testId, fromUomId);
        if (conversion != null) {
            conversionCache.put(cacheKey, conversion);
        }

        return conversion;
    }
}
