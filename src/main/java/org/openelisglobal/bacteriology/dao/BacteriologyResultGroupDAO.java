package org.openelisglobal.bacteriology.dao;

import java.util.List;
import org.openelisglobal.bacteriology.valueholder.BacteriologyResultGroup;
import org.openelisglobal.common.dao.BaseDAO;

public interface BacteriologyResultGroupDAO extends BaseDAO<BacteriologyResultGroup, Integer> {

    /**
     * Get all result groups for an analysis
     */
    List<BacteriologyResultGroup> getGroupsByAnalysisId(Integer analysisId);

    /**
     * Get groups by type for an analysis
     */
    List<BacteriologyResultGroup> getGroupsByAnalysisAndType(Integer analysisId, String groupType);

    /**
     * Get the culture group for an analysis
     */
    BacteriologyResultGroup getCultureGroupForAnalysis(Integer analysisId);

    /**
     * Get child groups of a parent group
     */
    List<BacteriologyResultGroup> getChildGroups(Integer parentGroupId);

    /**
     * Delete all groups for an analysis (soft delete - set is_active = false)
     */
    void deactivateGroupsForAnalysis(Integer analysisId);

    /**
     * @param object the data to insert
     * @return the id of the inserted object
     */
    Integer insert(BacteriologyResultGroup object);

    /**
     * @param object the new data to update the database with.
     * @return the object as it was saved to the database
     */
    BacteriologyResultGroup update(BacteriologyResultGroup baseObject);
}
