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
import java.util.Map;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedFormula;

/**
 * Service for evaluating derived formulas to calculate SI values.
 */
public interface FormulaEvaluationService {

    /**
     * Evaluate a derived formula using SI values from source test results.
     *
     * @param formula       The derived formula to evaluate
     * @param sourceResults Map of source test to result (should contain SI values)
     * @return The calculated SI value, or null if evaluation fails
     */
    BigDecimal evaluateFormula(TestDerivedFormula formula, Map<Test, Result> sourceResults);

    /**
     * Clear the alias resolution cache. Call this when test aliases are modified.
     */
    void clearCache();
}
