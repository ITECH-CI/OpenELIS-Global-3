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
import org.openelisglobal.testunitconversion.dao.TestUnitConversionDAO;
import org.openelisglobal.testunitconversion.valueholder.TestUnitConversion;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for TestUnitConversion entity.
 */
@Component
@Transactional
public class TestUnitConversionDAOImpl extends BaseDAOImpl<TestUnitConversion, String>
        implements TestUnitConversionDAO {

    public TestUnitConversionDAOImpl() {
        super(TestUnitConversion.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TestUnitConversion findByTestAndFromUom(String testId, String fromUomId) {
        try {
            String hql = "FROM TestUnitConversion tuc WHERE tuc.test.id = :testId "
                    + "AND tuc.fromUom.id = :fromUomId AND tuc.active = true";
            Query<TestUnitConversion> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestUnitConversion.class);
            query.setParameter("testId", Integer.valueOf(testId));
            query.setParameter("fromUomId", Integer.valueOf(fromUomId));
            query.setMaxResults(1);

            List<TestUnitConversion> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestUnitConversionDAO findByTestAndFromUom()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestUnitConversion> findAllActive() {
        try {
            String hql = "FROM TestUnitConversion tuc WHERE tuc.active = true ORDER BY tuc.test.id";
            Query<TestUnitConversion> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestUnitConversion.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestUnitConversionDAO findAllActive()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestUnitConversion> findByTestId(String testId) {
        try {
            String hql = "FROM TestUnitConversion tuc WHERE tuc.test.id = :testId ORDER BY tuc.fromUom.id";
            Query<TestUnitConversion> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestUnitConversion.class);
            query.setParameter("testId", Integer.valueOf(testId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestUnitConversionDAO findByTestId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestUnitConversion> getAll() {
        try {
            String hql = "SELECT DISTINCT tuc FROM TestUnitConversion tuc " + "LEFT JOIN FETCH tuc.test "
                    + "LEFT JOIN FETCH tuc.fromUom " + "LEFT JOIN FETCH tuc.toUom " + "ORDER BY tuc.test.id";
            Query<TestUnitConversion> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestUnitConversion.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestUnitConversionDAO getAll()", e);
        }
    }
}
