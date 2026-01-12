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
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testunitconversion.valueholder.TestAlias;

/**
 * DAO interface for TestAlias entity.
 */
public interface TestAliasDAO extends BaseDAO<TestAlias, String> {

    /**
     * Find test by alias name.
     *
     * @param alias Alias name
     * @return The test associated with the alias, or null if not found
     */
    Test findTestByAlias(String alias);

    /**
     * Get all aliases for a specific test.
     *
     * @param testId Test ID
     * @return List of aliases for the test
     */
    List<TestAlias> findByTestId(String testId);

    /**
     * Get all test aliases.
     *
     * @return List of all test aliases
     */
    List<TestAlias> findAll();
}
