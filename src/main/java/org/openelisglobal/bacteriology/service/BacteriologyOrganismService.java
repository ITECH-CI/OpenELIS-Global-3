package org.openelisglobal.bacteriology.service;

import java.util.List;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionary.valueholder.Dictionary;

public interface BacteriologyOrganismService extends BaseObjectService<BacteriologyOrganism, Integer> {

    /**
     * Save or update an organism
     *
     * @param organism The organism to save
     * @return The saved organism with ID
     */
    BacteriologyOrganism save(BacteriologyOrganism organism);

    /**
     * Get organism by result group ID
     *
     * @param resultGroupId The result group ID
     * @return The organism or null
     */
    BacteriologyOrganism getByGroupId(Integer resultGroupId);

    /**
     * Get all organisms for an analysis
     *
     * @param analysisId The analysis ID
     * @return List of organisms
     */
    List<BacteriologyOrganism> getOrganismsByAnalysisId(Integer analysisId);

    /**
     * Deactivate all organisms for an analysis (soft delete)
     *
     * @param analysisId The analysis ID
     */
    void deactivateOrganismsForAnalysis(Integer analysisId);

    /**
     * Delete an organism (soft delete by setting isActive to false)
     *
     * @param id        The organism ID
     * @param sysUserId The user ID performing the operation
     */
    void deleteOrganism(Integer id, String sysUserId);

    /**
     * Get all organism names from dictionary
     *
     * @return List of organism name dictionary entries
     */
    List<Dictionary> getAllOrganismNames();

    /**
     * Get all organism names sorted
     *
     * @return List of organism name dictionary entries sorted
     */
    List<Dictionary> getAllOrganismNamesSorted();

}
