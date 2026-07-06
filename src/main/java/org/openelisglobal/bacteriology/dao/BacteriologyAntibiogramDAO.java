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
     * Get all active antibiogram results for a set of organisms in a single query
     * (batch fetch to avoid N+1 when rendering reports).
     */
    List<BacteriologyAntibiogram> getAntibiogramsByOrganismIds(List<Integer> organismIds);

    /**
     * Get antibiogram for a specific organism and antibiotic
     */
    BacteriologyAntibiogram getByOrganismAndAntibiotic(Integer organismId, Integer antibioticDictId);

    /**
     * Delete all antibiograms for an analysis (soft delete)
     */
    void deactivateAntibiogramsForAnalysis(Integer analysisId);
}
