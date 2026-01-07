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
import org.openelisglobal.testunitconversion.dao.TestDerivedDependencyDAO;
import org.openelisglobal.testunitconversion.valueholder.TestDerivedDependency;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for TestDerivedDependency entity.
 */
@Component
@Transactional
public class TestDerivedDependencyDAOImpl extends BaseDAOImpl<TestDerivedDependency, String>
        implements TestDerivedDependencyDAO {

    public TestDerivedDependencyDAOImpl() {
        super(TestDerivedDependency.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestDerivedDependency> findByFormulaId(String formulaId) {
        try {
            String hql = "SELECT DISTINCT tdd FROM TestDerivedDependency tdd " + "LEFT JOIN FETCH tdd.sourceTest "
                    + "WHERE tdd.derivedFormula.id = :formulaId";
            Query<TestDerivedDependency> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestDerivedDependency.class);
            query.setParameter("formulaId", Integer.valueOf(formulaId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestDerivedDependencyDAO findByFormulaId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestDerivedDependency> findAll() {
        try {
            String hql = "FROM TestDerivedDependency tdd ORDER BY tdd.derivedFormula.id";
            Query<TestDerivedDependency> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TestDerivedDependency.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestDerivedDependencyDAO findAll()", e);
        }
    }
}
