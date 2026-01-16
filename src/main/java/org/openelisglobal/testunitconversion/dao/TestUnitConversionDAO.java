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
import org.openelisglobal.testunitconversion.valueholder.TestUnitConversion;

/**
 * DAO interface for TestUnitConversion entity.
 */
public interface TestUnitConversionDAO extends BaseDAO<TestUnitConversion, String> {

    /**
     * Find active conversion rule for a specific test and source unit.
     *
     * @param testId    Test ID
     * @param fromUomId Source unit of measure ID
     * @return The conversion rule, or null if not found
     */
    TestUnitConversion findByTestAndFromUom(String testId, String fromUomId);

    /**
     * Get all active conversion rules.
     *
     * @return List of active conversion rules
     */
    List<TestUnitConversion> findAllActive();

    /**
     * Get all conversion rules for a specific test.
     *
     * @param testId Test ID
     * @return List of conversion rules for the test
     */
    List<TestUnitConversion> findByTestId(String testId);
}
