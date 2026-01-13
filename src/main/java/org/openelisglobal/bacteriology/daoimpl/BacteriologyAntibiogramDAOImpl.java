package org.openelisglobal.bacteriology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.bacteriology.dao.BacteriologyAntibiogramDAO;
import org.openelisglobal.bacteriology.valueholder.BacteriologyAntibiogram;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BacteriologyAntibiogramDAOImpl extends BaseDAOImpl<BacteriologyAntibiogram, Integer>
        implements BacteriologyAntibiogramDAO {

    public BacteriologyAntibiogramDAOImpl() {
        super(BacteriologyAntibiogram.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyAntibiogram> getAntibiogramsByOrganismId(Integer organismId) {
        try {
            String hql = "FROM BacteriologyAntibiogram ba WHERE ba.organismId = :organismId "
                    + "AND ba.isActive = true";
            Query<BacteriologyAntibiogram> query = entityManager.unwrap(Session.class).createQuery(hql,
                    BacteriologyAntibiogram.class);
            query.setParameter("organismId", organismId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getAntibiogramsByOrganismId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BacteriologyAntibiogram getByOrganismAndAntibiotic(Integer organismId, Integer antibioticDictId) {
        try {
            String hql = "FROM BacteriologyAntibiogram ba WHERE ba.organismId = :organismId "
                    + "AND ba.antibioticDictId = :antibioticDictId AND ba.isActive = true";
            Query<BacteriologyAntibiogram> query = entityManager.unwrap(Session.class).createQuery(hql,
                    BacteriologyAntibiogram.class);
            query.setParameter("organismId", organismId);
            query.setParameter("antibioticDictId", antibioticDictId);
            List<BacteriologyAntibiogram> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getByOrganismAndAntibiotic", e);
        }
    }

    @Override
    public void deactivateAntibiogramsForAnalysis(Integer analysisId) {
        try {
            String hql = "UPDATE BacteriologyAntibiogram ba SET ba.isActive = false " +
                    "WHERE ba.organismId IN " +
                    "(SELECT bo.id FROM BacteriologyOrganism bo " +
                    "WHERE bo.resultGroupId IN " +
                    "(SELECT brg.id FROM BacteriologyResultGroup brg WHERE brg.analysisId = :analysisId))";
            Query query = entityManager.unwrap(Session.class).createQuery(hql);
            query.setParameter("analysisId", analysisId);
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in deactivateAntibiogramsForAnalysis", e);
        }
    }
}
