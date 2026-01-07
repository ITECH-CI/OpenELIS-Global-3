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
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testunitconversion.dao.TestAliasDAO;
import org.openelisglobal.testunitconversion.valueholder.TestAlias;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for TestAlias entity.
 */
@Component
@Transactional
public class TestAliasDAOImpl extends BaseDAOImpl<TestAlias, String> implements TestAliasDAO {

    public TestAliasDAOImpl() {
        super(TestAlias.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Test findTestByAlias(String alias) {
        try {
            String hql = "SELECT ta.test FROM TestAlias ta WHERE UPPER(ta.alias) = UPPER(:alias)";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(hql, Test.class);
            query.setParameter("alias", alias);
            query.setMaxResults(1);

            List<Test> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestAliasDAO findTestByAlias()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAlias> findByTestId(String testId) {
        try {
            String hql = "FROM TestAlias ta WHERE ta.test.id = :testId ORDER BY ta.alias";
            Query<TestAlias> query = entityManager.unwrap(Session.class).createQuery(hql, TestAlias.class);
            query.setParameter("testId", testId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestAliasDAO findByTestId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAlias> findAll() {
        try {
            String hql = "FROM TestAlias ta ORDER BY ta.test.id, ta.alias";
            Query<TestAlias> query = entityManager.unwrap(Session.class).createQuery(hql, TestAlias.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestAliasDAO findAll()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAlias> getAll() {
        try {
            String hql = "SELECT DISTINCT ta FROM TestAlias ta " + "LEFT JOIN FETCH ta.test "
                    + "ORDER BY ta.test.id, ta.alias";
            Query<TestAlias> query = entityManager.unwrap(Session.class).createQuery(hql, TestAlias.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestAliasDAO getAll()", e);
        }
    }
}
