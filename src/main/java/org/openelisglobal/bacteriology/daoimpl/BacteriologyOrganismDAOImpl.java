package org.openelisglobal.bacteriology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.bacteriology.dao.BacteriologyOrganismDAO;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BacteriologyOrganismDAOImpl extends BaseDAOImpl<BacteriologyOrganism, Integer>
        implements BacteriologyOrganismDAO {

    public BacteriologyOrganismDAOImpl() {
        super(BacteriologyOrganism.class);
    }

    @Override
    @Transactional(readOnly = true)
    public BacteriologyOrganism getByGroupId(Integer resultGroupId) {
        try {
            String hql = "FROM BacteriologyOrganism bo WHERE bo.resultGroupId = :resultGroupId "
                    + "AND bo.isActive = true";
            Query<BacteriologyOrganism> query = entityManager.unwrap(Session.class).createQuery(hql,
                    BacteriologyOrganism.class);
            query.setParameter("resultGroupId", resultGroupId);
            List<BacteriologyOrganism> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getByGroupId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyOrganism> getOrganismsByAnalysisId(Integer analysisId) {
        try {
            // Use subquery since there's no direct association between BacteriologyOrganism
            // and BacteriologyResultGroup
            String hql = "SELECT bo FROM BacteriologyOrganism bo " + "WHERE bo.resultGroupId IN ("
                    + "  SELECT brg.id FROM BacteriologyResultGroup brg WHERE brg.analysisId = :analysisId"
                    + ") AND bo.isActive = true " + "ORDER BY bo.organismNumber";
            Query<BacteriologyOrganism> query = entityManager.unwrap(Session.class).createQuery(hql,
                    BacteriologyOrganism.class);
            query.setParameter("analysisId", analysisId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getOrganismsByAnalysisId", e);
        }
    }

    @Override
    public void deactivateOrganismsForAnalysis(Integer analysisId) {
        try {
            String hql = "UPDATE BacteriologyOrganism bo SET bo.isActive = false " + "WHERE bo.resultGroupId IN "
                    + "(SELECT brg.id FROM BacteriologyResultGroup brg WHERE brg.analysisId = :analysisId)";
            Query query = entityManager.unwrap(Session.class).createQuery(hql);
            query.setParameter("analysisId", analysisId);
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in deactivateOrganismsForAnalysis", e);
        }
    }
}
