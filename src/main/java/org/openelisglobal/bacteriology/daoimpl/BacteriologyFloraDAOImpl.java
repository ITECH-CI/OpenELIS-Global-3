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
 * <p>Copyright (C) ITECH-CI. All Rights Reserved.
 */
package org.openelisglobal.bacteriology.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.bacteriology.dao.BacteriologyFloraDAO;
import org.openelisglobal.bacteriology.valueholder.BacteriologyFlora;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * BacteriologyFloraDAOImpl - Implementation of Data Access Object for bacterial
 * flora
 */
@Component
@Transactional
public class BacteriologyFloraDAOImpl extends BaseDAOImpl<BacteriologyFlora, Long> implements BacteriologyFloraDAO {

    @PersistenceContext
    private EntityManager entityManager;

    public BacteriologyFloraDAOImpl() {
        super(BacteriologyFlora.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyFlora> getByAnalysisId(Integer analysisId) throws LIMSRuntimeException {
        try {
            String hql = "FROM BacteriologyFlora bf " + "LEFT JOIN FETCH bf.details "
                    + "WHERE bf.analysisId = :analysisId " + "ORDER BY bf.floraCountTestId";

            Query<BacteriologyFlora> query = entityManager.unwrap(Session.class).createQuery(hql,
                    BacteriologyFlora.class);
            query.setParameter("analysisId", analysisId);

            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BacteriologyFlora getByAnalysisId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BacteriologyFlora getByAnalysisIdAndTestId(Integer analysisId, Integer floraCountTestId)
            throws LIMSRuntimeException {
        try {
            String hql = "FROM BacteriologyFlora bf " + "LEFT JOIN FETCH bf.details "
                    + "WHERE bf.analysisId = :analysisId " + "AND bf.floraCountTestId = :testId";

            Query<BacteriologyFlora> query = entityManager.unwrap(Session.class).createQuery(hql,
                    BacteriologyFlora.class);
            query.setParameter("analysisId", analysisId);
            query.setParameter("testId", floraCountTestId);

            List<BacteriologyFlora> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BacteriologyFlora getByAnalysisIdAndTestId()", e);
        }
    }

    @Override
    @Transactional
    public void deleteByAnalysisId(Integer analysisId) throws LIMSRuntimeException {
        try {
            // First, fetch all flora records for this analysis
            List<BacteriologyFlora> floraList = getByAnalysisId(analysisId);

            // Delete each one (cascade will handle details)
            Session session = entityManager.unwrap(Session.class);
            for (BacteriologyFlora flora : floraList) {
                session.delete(flora);
            }
            session.flush();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BacteriologyFlora deleteByAnalysisId()", e);
        }
    }

    @Override
    @Transactional
    public void deleteByAnalysisIdAndTestId(Integer analysisId, Integer floraCountTestId) throws LIMSRuntimeException {
        try {
            BacteriologyFlora flora = getByAnalysisIdAndTestId(analysisId, floraCountTestId);
            if (flora != null) {
                Session session = entityManager.unwrap(Session.class);
                session.delete(flora);
                session.flush();
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BacteriologyFlora deleteByAnalysisIdAndTestId()", e);
        }
    }
}
