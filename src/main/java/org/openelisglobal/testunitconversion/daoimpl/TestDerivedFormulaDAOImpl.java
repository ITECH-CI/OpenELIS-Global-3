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
package org.openelisglobal.testunitconversion.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.testunitconversion.dao.TestDerivedFormulaDAO;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedFormula;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for TestDerivedFormula entity.
 */
@Component
@Transactional
public class TestDerivedFormulaDAOImpl extends BaseDAOImpl<TestDerivedFormula, String>
        implements TestDerivedFormulaDAO {

    public TestDerivedFormulaDAOImpl() {
        super(TestDerivedFormula.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TestDerivedFormula findByTestId(String testId) {
        try {
            String hql = "FROM TestDerivedFormula tdf WHERE tdf.test.id = :testId AND tdf.active = true";
            Query<TestDerivedFormula> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestDerivedFormula.class);
            query.setParameter("testId", Integer.valueOf(testId));
            query.setMaxResults(1);

            List<TestDerivedFormula> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestDerivedFormulaDAO findByTestId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestDerivedFormula> findAllActive() {
        try {
            String hql = "FROM TestDerivedFormula tdf WHERE tdf.active = true ORDER BY tdf.test.id";
            Query<TestDerivedFormula> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestDerivedFormula.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestDerivedFormulaDAO findAllActive()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestDerivedFormula> findBySourceTestId(String sourceTestId) {
        try {
            String hql = "SELECT DISTINCT tdf FROM TestDerivedFormula tdf " + "LEFT JOIN FETCH tdf.dependencies dep "
                    + "WHERE dep.sourceTest.id = :sourceTestId AND tdf.active = true";
            Query<TestDerivedFormula> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestDerivedFormula.class);
            query.setParameter("sourceTestId", Integer.valueOf(sourceTestId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestDerivedFormulaDAO findBySourceTestId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestDerivedFormula> getAll() {
        try {
            String hql = "SELECT DISTINCT tdf FROM TestDerivedFormula tdf " + "LEFT JOIN FETCH tdf.test "
                    + "LEFT JOIN FETCH tdf.fromUom " + "LEFT JOIN FETCH tdf.toUomSi " + "ORDER BY tdf.test.id";
            Query<TestDerivedFormula> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestDerivedFormula.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestDerivedFormulaDAO getAll()", e);
        }
    }
}
