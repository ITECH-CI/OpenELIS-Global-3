package org.openelisglobal.bacteriology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.bacteriology.dao.BacteriologyResultGroupDAO;
import org.openelisglobal.bacteriology.valueholder.BacteriologyResultGroup;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BacteriologyResultGroupDAOImpl extends BaseDAOImpl<BacteriologyResultGroup, Integer>
        implements BacteriologyResultGroupDAO {

    public BacteriologyResultGroupDAOImpl() {
        super(BacteriologyResultGroup.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyResultGroup> getGroupsByAnalysisId(Integer analysisId) {
        try {
            String hql = "FROM BacteriologyResultGroup brg WHERE brg.analysisId = :analysisId "
                    + "AND brg.isActive = true ORDER BY brg.displayOrder";
            Query<BacteriologyResultGroup> query = entityManager.unwrap(Session.class).createQuery(hql,
                    BacteriologyResultGroup.class);
            query.setParameter("analysisId", analysisId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getGroupsByAnalysisId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyResultGroup> getGroupsByAnalysisAndType(Integer analysisId, String groupType) {
        try {
            String hql = "FROM BacteriologyResultGroup brg WHERE brg.analysisId = :analysisId "
                    + "AND brg.groupType = :groupType AND brg.isActive = true ORDER BY brg.displayOrder";
            Query<BacteriologyResultGroup> query = entityManager.unwrap(Session.class).createQuery(hql,
                    BacteriologyResultGroup.class);
            query.setParameter("analysisId", analysisId);
            query.setParameter("groupType", groupType);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getGroupsByAnalysisAndType", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BacteriologyResultGroup getCultureGroupForAnalysis(Integer analysisId) {
        try {
            List<BacteriologyResultGroup> groups = getGroupsByAnalysisAndType(analysisId, "CULTURE");
            return groups.isEmpty() ? null : groups.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getCultureGroupForAnalysis", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyResultGroup> getChildGroups(Integer parentGroupId) {
        try {
            String hql = "FROM BacteriologyResultGroup brg WHERE brg.parentGroupId = :parentGroupId "
                    + "AND brg.isActive = true ORDER BY brg.displayOrder";
            Query<BacteriologyResultGroup> query = entityManager.unwrap(Session.class).createQuery(hql,
                    BacteriologyResultGroup.class);
            query.setParameter("parentGroupId", parentGroupId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getChildGroups", e);
        }
    }

    @Override
    public void deactivateGroupsForAnalysis(Integer analysisId) {
        try {
            String hql = "UPDATE BacteriologyResultGroup brg SET brg.isActive = false WHERE brg.analysisId = :analysisId";
            Query query = entityManager.unwrap(Session.class).createQuery(hql);
            query.setParameter("analysisId", analysisId);
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in deactivateGroupsForAnalysis", e);
        }
    }
}
