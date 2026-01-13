package org.openelisglobal.bacteriology.dao;

import java.util.List;
import org.openelisglobal.bacteriology.valueholder.BacteriologyAntibiogram;
import org.openelisglobal.common.dao.BaseDAO;

public interface BacteriologyAntibiogramDAO extends BaseDAO<BacteriologyAntibiogram, Integer> {

    /**
     * Get all antibiogram results for an organism
     */
    List<BacteriologyAntibiogram> getAntibiogramsByOrganismId(Integer organismId);

    /**
     * Get antibiogram for a specific organism and antibiotic
     */
    BacteriologyAntibiogram getByOrganismAndAntibiotic(Integer organismId, Integer antibioticDictId);

    /**
     * Delete all antibiograms for an analysis (soft delete)
     */
    void deactivateAntibiogramsForAnalysis(Integer analysisId);
}
