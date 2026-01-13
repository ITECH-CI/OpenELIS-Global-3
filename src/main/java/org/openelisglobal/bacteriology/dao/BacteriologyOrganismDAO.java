package org.openelisglobal.bacteriology.dao;

import java.util.List;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.common.dao.BaseDAO;

public interface BacteriologyOrganismDAO extends BaseDAO<BacteriologyOrganism, Integer> {

    /**
     * Get organism by result group ID
     */
    BacteriologyOrganism getByGroupId(Integer resultGroupId);

    /**
     * Get all organisms for an analysis
     */
    List<BacteriologyOrganism> getOrganismsByAnalysisId(Integer analysisId);

    /**
     * Delete all organisms for an analysis (soft delete)
     */
    void deactivateOrganismsForAnalysis(Integer analysisId);
}
