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
package org.openelisglobal.testunitconversion.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedFormula;

/**
 * DAO interface for TestDerivedFormula entity.
 */
public interface TestDerivedFormulaDAO extends BaseDAO<TestDerivedFormula, String> {

    /**
     * Find active derived formula for a specific test.
     *
     * @param testId Test ID
     * @return The derived formula, or null if not found
     */
    TestDerivedFormula findByTestId(String testId);

    /**
     * Get all active derived formulas.
     *
     * @return List of active derived formulas
     */
    List<TestDerivedFormula> findAllActive();

    /**
     * Find derived formulas that depend on a specific source test.
     *
     * @param sourceTestId Source test ID
     * @return List of derived formulas that use this test
     */
    List<TestDerivedFormula> findBySourceTestId(String sourceTestId);
}
